#!/usr/bin/env python3
"""
TDB Development Stage Report Generator

Processes tab-separated tdb data and generates a report showing development 
stage by filename, tester, and platform for a specific contract year.
"""

import sys
from datetime import datetime
from collections import defaultdict, namedtuple

# Constants
PLACEHOLDER = ".."

VALID_STATUSES = {
    'exists', 'expected', 'down', 'ingNotReady', 'notReady',
    'manifest', 'testing', 'ready', 'wanted', 'crawling',
    'deepCrawl', 'frozen', 'zapped', 'finished'
}

STAGE1_PROBLEM_STATUSES = {'notReady', 'down', 'ingNotReady'}
STAGE1_EARLY_STATUSES = {'exists', 'expected'}
STAGE3_ADVANCED_STATUSES = {'crawling', 'deepCrawl', 'frozen', 'finished', 'zapped'}
STAGE1_PROBLEM_PLUGINS = {'needs.plugin', 'noplugin'}

# Named tuple for items
Item = namedtuple('Item', ['plugin', 'year', 'status', 'contract_year'])


def is_empty(field):
    """Check if a field is empty or contains only whitespace."""
    return not field or not field.strip()


def parse_year(year_str):
    """
    Parse year string, handling ranges like '2020-2022'.
    Returns the end year as an integer, or None if invalid.
    """
    try:
        return int(year_str.split('-')[-1])
    except (ValueError, AttributeError):
        return None


def should_include_item(year, contract, contract_year_input, status):
    """
    Determine if an item should be included in processing.
    Returns (True/False, error_message).
    """
    if is_empty(contract):
        return False, "missing contract year"
    
    if contract != contract_year_input:
        return False, "contract year mismatch"
    
    parsed_year = parse_year(year)
    if parsed_year is None:
        return False, "invalid year"
    
    try:
        if parsed_year >= int(contract):
            return False, "year >= contract year"
    except ValueError:
        return False, "invalid contract year format"
    
    if status not in VALID_STATUSES:
        return False, "invalid status"
    
    return True, None


def check_platform_override(platform):
    """
    Check if platform contains stage override marker.
    Returns stage number (1, 2, 3) or None.
    """
    if "STAGE=1" in platform:
        return 1
    elif "STAGE=2" in platform:
        return 2
    elif "STAGE=3" in platform:
        return 3
    return None


def all_statuses_in(item_statuses, valid_set):
    """Check if all item statuses are in the valid set."""
    return item_statuses and item_statuses <= valid_set


def calculate_stage(items, tester, platform):
    """
    Calculate development stage for a group of items.
    Returns stage number (0-3).
    """
    # 1. Check platform override
    override = check_platform_override(platform)
    if override is not None:
        return override
    
    # 2. Check critical missing fields
    for item in items:
        if is_empty(item.year) or is_empty(item.contract_year):
            return 1
    
    # 3. Check tester validity
    if tester != "5" and tester != "8":
        return 1
    
    # 4. Check plugin issues
    for item in items:
        if is_empty(item.plugin) or item.plugin in STAGE1_PROBLEM_PLUGINS:
            return 1
    
    # 5. Collect statuses
    item_statuses = {item.status for item in items}
    
    # 6. Check problem statuses
    if item_statuses & STAGE1_PROBLEM_STATUSES:
        return 1
    
    # 7. Check early statuses
    if all_statuses_in(item_statuses, STAGE1_EARLY_STATUSES):
        return 1
    
    # 8. Check advanced statuses
    if all_statuses_in(item_statuses, STAGE3_ADVANCED_STATUSES):
        return 3
    
    # 9. Default
    return 2


def main():
    """Main processing function."""
    # Check command-line arguments
    if len(sys.argv) != 2:
        print("Usage: python3 script.py <contract_year>", file=sys.stderr)
        print("Example: python3 script.py 2025", file=sys.stderr)
        sys.exit(1)
    
    contract_year_input = sys.argv[1]
    
    # Data structures
    groups = defaultdict(list)  # (filename, tester, platform) -> [Item, ...]
    all_filenames = set()
    filenames_with_items = set()
    
    # Process input from stdin
    for line_num, line in enumerate(sys.stdin, start=1):
        line = line.rstrip('\n')
        
        # Skip empty lines
        if not line.strip():
            continue
        
        # Parse line
        try:
            publisher, plugin, year, tester, contract, status, platform, filename = line.split('\t')
        except ValueError:
            print(f"Warning: Line {line_num}: Malformed line, expected 8 tab-separated fields", 
                  file=sys.stderr)
            continue
        
        all_filenames.add(filename)
        
        # Filter item
        should_include, reason = should_include_item(year, contract, contract_year_input, status)
        if not should_include:
            continue
        
        # Normalize empty fields to placeholder
        if is_empty(tester):
            tester = PLACEHOLDER
        if is_empty(platform):
            platform = PLACEHOLDER
        
        # Create item and add to group
        item = Item(plugin, year, status, contract)
        key = (filename, tester, platform)
        groups[key].append(item)
        filenames_with_items.add(filename)
    
    # Build results
    results = []
    
    # Process all groups
    for (filename, tester, platform), items in groups.items():
        stage = calculate_stage(items, tester, platform)
        results.append((stage, filename, tester, platform))
    
    # Add stage 0 for filenames with no items
    stage0_filenames = all_filenames - filenames_with_items
    for filename in stage0_filenames:
        results.append((0, filename, PLACEHOLDER, PLACEHOLDER))
    
    # Sort by stage, filename, tester, platform
    results.sort(key=lambda x: (x[0], x[1], x[2], x[3]))
    
    # Output header
    print(f"Stage-{contract_year_input}\tFilename\tTester\tNote")
    
    # Output results
    for stage, filename, tester, platform in results:
        print(f"{stage}\t{filename}\t{tester}\t{platform}")
    
    # Output timestamp
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"Generated: {timestamp}")


if __name__ == "__main__":
    main()