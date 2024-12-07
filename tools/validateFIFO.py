
import os
import sys

def validate_fifo(file_path, n_procs):
    # Check if the file is empty
    if os.stat(file_path).st_size == 0:
        print("wrong file: ", file_path, " | reason: empty file")
        return False
    
    correct_order = True
    # An array of size n_procs to store the expected sequence number of each process starting from 1
    expected_seq = [1] * int(n_procs)

    with open(file_path, 'r') as file:
        for i,line in enumerate(file):
            if not line.startswith("DELIVERED"):
                continue

            splitted_line = line.split()
            # Extract the sequence number and the process number from the line
            proc_num = int(splitted_line[1])
            seq_num = int(splitted_line[2])

            # Check if the sequence number is as expected
            if seq_num != expected_seq[proc_num-1]:
                print("wrong file: ", file_path, " | reason: wrong sequence number at line ", i+1)
                correct_order = False
                break

            # Update the expected sequence number of the process
            expected_seq[proc_num-1] += 1

    return correct_order
    


def main():
    stress_logs_dir = "../stressLogs"

    correct_order = True
    logs_list = os.listdir(stress_logs_dir)
    for file_name in logs_list:
        if file_name.startswith("proc") and file_name.endswith(".output"):
            file_path = os.path.join(stress_logs_dir, file_name)
            if not validate_fifo(file_path, len(logs_list) - 2):
                correct_order = False

    if correct_order:
        print("good output")
    else:
        print("bad output")

if __name__ == "__main__":
    main()