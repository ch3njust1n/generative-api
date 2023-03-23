"""
File system information ingestion
"""

import time


def main():
    start = time.perf_counter()
    system_info = get_system_info()
    print(system_info)
    print(f"{time.perf_counter() - start} s")

    start = time.perf_counter()
    file_tree = map_file_system()
    print(file_tree)
    print(f"{time.perf_counter() - start} s")


if __name__ == "__main__":
    main()
