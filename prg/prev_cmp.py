import sys
import re


def main():
    if len(sys.argv) < 3:
        print('usage: python3 prev_cmp.py [file1] [file2]')
        exit(1)

    file1 = ''.join(open(sys.argv[1]).readlines())
    file2 = ''.join(open(sys.argv[2]).readlines())
    file1 = re.sub(r'\(\d+\)', '', file1);
    file2 = re.sub(r'\(\d+\)', '', file2);

    if file1 == file2:
        print('Equal')
    else:
        print('Different')


if __name__ == '__main__':
    main()

