#!/usr/bin/env python3
"""
TDB Development Stage Report Generator

Processes tab-separated tdb data and generates a report showing development 
stage by contract year, filename, tester, and platform for all contract years 
found in the input.
"""

import sys
from datetime import datetime
from collections import defaultdict, namedtuple

# Constants
PLACEHOLDER = ".."
NO_VALID_ITEMS = "No valid input to consider"

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


def should_include_item(year, contract, status):
    """
    Determine if an item should be included in processing.
    Returns (True/False, error_message).
    Only filters by status and year comparison.
    Items with missing fields are kept (handled in stage calculation).
    """
    # Filter by status
    if status not in VALID_STATUSES:
        return False, "invalid status"
    
    # If contract_year or year is missing/invalid, keep the item
    # (will be handled in stage calculation)
    if is_empty(contract) or is_empty(year):
        return True, None
    
    parsed_year = parse_year(year)
    parsed_contract = parse_year(contract)
    
    if parsed_year is None or parsed_contract is None:
        return True, None
    
    # Filter if year >= contract_year
    if parsed_year >= parsed_contract:
        return False, "year >= contract year"
    
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


def calculate_stage(items, contract_year, tester, platform):
    """
    Calculate development stage for a group of items.
    Returns stage number (0-3).
    """
    # 0. Check for missing contract_year
    if contract_year == PLACEHOLDER:
        return 0
    
    # 1. Check platform override
    override = check_platform_override(platform)
    if override is not None:
        return override
    
    # 2. Check critical missing fields (year or tester)
    for item in items:
        if is_empty(item.year):
            return 1
    
    if tester == PLACEHOLDER:
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
    # Data structures
    groups = defaultdict(list)  # (contract, filename, tester, platform) -> [Item, ...]
    all_contract_year_filename_pairs = set()  # All (contract_year, filename) seen
    valid_contract_year_filename_pairs = set()  # Pairs with valid items
    
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
        
        # Normalize contract_year for tracking
        contract_for_tracking = contract if not is_empty(contract) else PLACEHOLDER
        all_contract_year_filename_pairs.add((contract_for_tracking, filename))
        
        # Filter item
        should_include, reason = should_include_item(year, contract, status)
        if not should_include:
            continue
        
        # Mark this pair as having valid items
        valid_contract_year_filename_pairs.add((contract_for_tracking, filename))
        
        # Normalize empty fields to placeholder
        if is_empty(contract):
            contract = PLACEHOLDER
        if is_empty(tester):
            tester = PLACEHOLDER
        if is_empty(platform):
            platform = PLACEHOLDER
        
        # Create item and add to group
        item = Item(plugin, year, status, contract)
        key = (contract, filename, tester, platform)
        groups[key].append(item)
    
    # Build results
    results = []
    
    # Process all groups
    for (contract, filename, tester, platform), items in groups.items():
        stage = calculate_stage(items, contract, tester, platform)
        results.append((contract, stage, filename, tester, platform))
    
    # Add "No valid input to consider" entries for filtered-out items
    filtered_out_pairs = all_contract_year_filename_pairs - valid_contract_year_filename_pairs
    for contract, filename in filtered_out_pairs:
        results.append((contract, 0, filename, PLACEHOLDER, NO_VALID_ITEMS))
    
    # Sort by contract_year, stage, filename, tester, platform
    results.sort(key=lambda x: (x[0], x[1], x[2], x[3], x[4]))
    
    # Output header
    print("Contract\tStage\tFilename\tTester\tNote")
    
    # Output results
    for contract, stage, filename, tester, platform in results:
        print(f"{contract}\t{stage}\t{filename}\t{tester}\t{platform}")
    
    # Output timestamp
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"Generated: {timestamp}")


if __name__ == "__main__":
    main()