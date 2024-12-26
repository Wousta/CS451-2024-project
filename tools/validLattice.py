import os

def is_subset(smaller, bigger):
    for elem in smaller:
        if elem not in bigger:
            return False
    return True


def is_superset(bigger, smaller):
    return is_subset(smaller, bigger)


def check_consistency(proposal1, proposal2):
    return is_subset(proposal1, proposal2) or is_subset(proposal2, proposal1)


# Merge a list of lists into a single list without duplicates
def merge_lists(lists):
    merged = []
    for l in lists:
        for elem in l:
            if elem not in merged:
                merged.append(elem)
    return merged


def read_lines(conf_fd, out_fd):
    confs = []
    outs = []
    for fd in out_fd:
        line = fd.readline()
        outs.append(line.strip().split(" "))

    if outs[0][0] == "DECIDING" or outs[0][0] == "delivered":
        return confs, outs
    
    for fd in conf_fd:
        line = fd.readline()
        confs.append(line.strip().split(" "))

    return confs, outs


def check_lattice_agreement(confs, outs, current_shot):
    merged_proposals = merge_lists(confs)

    for i in range(len(outs)):
        # Validity first condition
        if not is_subset(confs[i], outs[i]):
            print(f"SHOT {current_shot} Invalid lattice in {i+1}: {confs[i]} is not a subset of decided {outs[i]}")

        # Validity second condition   
        if not is_subset(outs[i], merged_proposals):
            print(f"SHOT {current_shot} Invalid lattice: {outs[i]} is not a subset of merged proposals {merged_proposals}")

        # Check consistency
        for j in range(len(outs)):
            if i != j and not check_consistency(outs[i], outs[j]):
                print(f"SHOT {current_shot} Invalid lattice: {outs[i]} and {outs[j]} are not comparable")


def main():
    stress_logs_dir = "../stressLogs"

    N_PROPOSALS_INDEX = 0

    conf_fd = []
    out_fd = []
    conf_params = []
    logs_list = os.listdir(stress_logs_dir)

    # Open the config and output files and put them in the corresponding lists
    for file_name in logs_list:
        file_path = os.path.join(stress_logs_dir, file_name)
        if file_name.startswith("proc") and file_name.endswith(".config"):
            conf_fd.append(open(file_path, 'r'))
        elif file_name.startswith("proc") and file_name.endswith(".output"):
            out_fd.append(open(file_path, 'r'))

    # Read the first line of the config files to get the number of proposals
    for i in range(len(conf_fd)):
        line = conf_fd[i].readline()
        if i == 0:
            splitted = list(map(int, line.strip().split(" ")))
            conf_params.extend(splitted)
       
    end = False            
    while not end:
        confs, outs = read_lines(conf_fd, out_fd)
        current_shot = 0

        # Check if the output line is a new shot announcement
        if outs[0][0] == "DECIDING":
            current_shot = int(outs[0][1])
            continue

        if outs[0][0] == "delivered":
            print("End of validity test")
            end = True
            break

        # Check lattice agreement properties
        check_lattice_agreement(confs, outs, current_shot)

        



if __name__ == "__main__":
    main()