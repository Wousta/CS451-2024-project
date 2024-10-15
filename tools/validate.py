import os
import re
import sys

def validate_file(file_path, check_double_number):
    # Check if the file is empty
    if os.stat(file_path).st_size == 0:
        print("wrong file: ", file_path, " | reason: empty file")
        return False
    
    flag = True
    with open(file_path, 'r') as file:
        for i,line in enumerate(file):
            if check_double_number:
                if not re.match(r'^[a-z] \d+ \d+', line.strip()):
                    flag = False
                    print("wrong file doub: ",file_path," line: ",i," | read: ",line.strip())
            else:
                if not re.match(r'^[a-z] \d+', line.strip()):
                    flag = False
                    print("wrong file:",file_path,"line: ",i,"| read:",line.strip())
    return flag

def check_no_duplicates(file_path):
    seen_lines = set()
    with open(file_path, 'r') as file:
        for i, line in enumerate(file):
            if line in seen_lines:
                print("wrong file:", file_path, "line:", i, "| reason: duplicate line")
                return False
            seen_lines.add(line)
    return True

def main():
    if len(sys.argv) != 2:
        print("Usage: python validate.py <index of receiver process with>")
        sys.exit(1)

    check_number = sys.argv[1]
    stress_logs_dir = "../stressLogs"

    for file_name in os.listdir(stress_logs_dir):
        if file_name.startswith("proc") and file_name.endswith(".output"):
            file_number = file_name[4:6]
            file_path = os.path.join(stress_logs_dir, file_name)
            check_double_number = (file_number == check_number)
            if not validate_file(file_path, check_double_number):
                return
    
    # Check repetitions in file with file number == check_number
    target_file = os.path.join(stress_logs_dir, f"proc{check_number}.output")
    try:
        if not check_no_duplicates(target_file):
            return
    except IOError:
        print("bad index given: use format XY to input the index of receiver file")
        return
    
    print("good output")

if __name__ == "__main__":
    main()