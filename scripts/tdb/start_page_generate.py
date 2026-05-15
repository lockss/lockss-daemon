#!/usr/bin/env python3
"""
LOCKSS Start Page Generator

This script fetches XML sitemaps from government websites and generates
HTML manifest pages for LOCKSS archival units.

Usage:
    python3 ./scripts/tdb/start_page_generate.py --input scripts/tdb/start_page_input.csv --acronyms scripts/tdb/usdocs_acronyms.txt --output /var/www/html/usdocs_start_urls --server http://uslockss-v1.rice.edu:24680
"""

import argparse
import csv
import os
import sys
import re
import time
import html
from datetime import datetime
from urllib.parse import quote
import requests
import xml.etree.ElementTree as ET


def parse_arguments():
    """Parse command-line arguments."""
    parser = argparse.ArgumentParser(
        description='Generate LOCKSS start pages from XML sitemaps'
    )
    parser.add_argument('--input', required=True,
                        help='Input CSV file with AUids and start URLs')
    parser.add_argument('--acronyms', required=True,
                        help='CSV file with collection acronyms and full names')
    parser.add_argument('--output', required=True,
                        help='Output directory for HTML files')
    parser.add_argument('--server', required=True,
                        help='Archive server base URL')
    return parser.parse_args()


def load_acronyms(acronym_file):
    """Load acronym to full name mappings from CSV file."""
    acronyms = {}
    try:
        with open(acronym_file, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if line:
                    parts = line.split(',', 1)
                    if len(parts) == 2:
                        acronyms[parts[0]] = parts[1]
    except Exception as e:
        print(f"Error loading acronym file: {e}", file=sys.stderr)
        sys.exit(1)
    return acronyms


def parse_auid(auid):
    """Extract collection_id and year from AUid string."""
    collection_match = re.search(r'collection_id~([^&]+)', auid)
    year_match = re.search(r'year~(\d+)', auid)
    
    collection_id = collection_match.group(1) if collection_match else None
    year = year_match.group(1) if year_match else None
    
    return collection_id, year


def fetch_xml(url):
    """Fetch XML content from URL."""
    try:
        response = requests.get(url, timeout=30)
        response.raise_for_status()
        return response.text
    except Exception as e:
        print(f"Error fetching {url}: {e}", file=sys.stderr)
        return None


def parse_sitemap_urls(xml_content):
    """Extract URLs from XML sitemap."""
    urls = []
    try:
        root = ET.fromstring(xml_content)
        # Handle namespace
        ns = {'sm': 'http://www.sitemaps.org/schemas/sitemap/0.9'}
        
        for url_elem in root.findall('sm:url', ns):
            loc_elem = url_elem.find('sm:loc', ns)
            if loc_elem is not None and loc_elem.text:
                urls.append(loc_elem.text)
    except Exception as e:
        print(f"Error parsing XML: {e}", file=sys.stderr)
    
    return urls


def extract_package_id(url):
    """Extract package ID from GovInfo details URL."""
    # Extract everything after the last '/'
    parts = url.rstrip('/').split('/')
    if parts:
        return parts[-1]
    return None


def fetch_title_from_mods(package_id):
    """Fetch title from MODS metadata file."""
    if not package_id:
        return None
    
    mods_url = f"https://www.govinfo.gov/metadata/pkg/{package_id}/mods.xml"
    
    try:
        time.sleep(0.5)  # Polite delay between requests
        response = requests.get(mods_url, timeout=30)
        response.raise_for_status()
        
        # Parse MODS XML
        root = ET.fromstring(response.text)
        
        # MODS namespace
        ns = {'mods': 'http://www.loc.gov/mods/v3'}
        
        # Try to find title in titleInfo/title
        title_elem = root.find('.//mods:titleInfo/mods:title', ns)
        
        if title_elem is not None and title_elem.text:
            # HTML-escape the title to handle special characters
            return html.escape(title_elem.text.strip())
        
        return None
        
    except Exception as e:
        print(f"  Warning: Could not fetch MODS for {package_id}: {e}", file=sys.stderr)
        return None


def create_transformed_url(server_base, auid, original_url):
    """Create transformed URL for LOCKSS archive server."""
    encoded_auid = quote(auid, safe='')
    encoded_url = quote(original_url, safe='')
    return f"{server_base}/ServeContent?auid={encoded_auid}&url={encoded_url}/context"


def generate_html_page(collection_name, year, urls, server_base, auid, output_file):
    """Generate HTML manifest page."""
    collection_id, _ = parse_auid(auid)
    
    html_content = f"""<!DOCTYPE html>
<html>
<head>
    <title>{collection_name} {year} LOCKSS Manifest Page</title>
</head>
<body>
    <h1>{collection_name} {year}<br>LOCKSS Manifest Page</h1>

    <ul>
"""
    
    for idx, url in enumerate(urls, 1):
        transformed_url = create_transformed_url(server_base, auid, url)
        
        # Try to fetch title from MODS
        package_id = extract_package_id(url)
        title = fetch_title_from_mods(package_id)
        
        # Use title if found, otherwise fall back to default format
        if title:
            link_text = title
        else:
            link_text = f"{collection_id} {year} URL {idx}"
        
        html_content += f'        <li><a href="{transformed_url}">{link_text}</a></li>\n'
    
    html_content += """    </ul>

    <p>LOCKSS system has permission to collect, preserve, and serve this Archival Unit</p>

    <a href="https://www.lockss.org/"><img src="lockss-program-400x118.png" alt="LOCKSS Logo"></a>
</body>
</html>
"""
    
    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(html_content)
        return True
    except Exception as e:
        print(f"Error writing HTML file {output_file}: {e}", file=sys.stderr)
        return False


def generate_index_page(entries, output_dir):
    """Generate index page with table of all manifest pages."""
    now = datetime.now().strftime("%a %b %d %H:%M:%S %Z %Y")
    
    html_content = f"""<!DOCTYPE html>
<html>
<head>
    <title>USDocs Start Pages Index</title>
    <style>
        table {{ border-collapse: collapse; margin-top: 20px; }}
        th, td {{ border: 1px solid #ddd; padding: 8px; text-align: left; }}
        th {{ background-color: #f2f2f2; }}
    </style>
</head>
<body>
    <h1>USDocs Start Pages Index</h1>

    <p>Last update {now}</p>

    <table>
        <tr>
            <th>Name</th>
            <th>Size</th>
            <th>Date Saved</th>
        </tr>
"""
    
    # Sort entries alphabetically by name, then by year
    sorted_entries = sorted(entries, key=lambda x: (x['name'], x['year']))
    
    for entry in sorted_entries:
        if entry['success']:
            file_path = os.path.join(output_dir, entry['filename'])
            try:
                file_size = os.path.getsize(file_path)
                file_time = datetime.fromtimestamp(os.path.getmtime(file_path))
                file_time_str = file_time.strftime("%Y-%m-%d %H:%M:%S")
                
                display_name = f"{entry['name']} {entry['year']}"
                html_content += f"""        <tr>
            <td><a href="{entry['filename']}">{display_name}</a></td>
            <td>{file_size}</td>
            <td>{file_time_str}</td>
        </tr>
"""
            except Exception as e:
                print(f"Error getting file info for {entry['filename']}: {e}", file=sys.stderr)
        else:
            display_name = f"{entry['name']} {entry['year']} (failed to fetch)"
            html_content += f"""        <tr>
            <td>{display_name}</td>
            <td>-</td>
            <td>-</td>
        </tr>
"""
    
    html_content += """    </table>
</body>
</html>
"""
    
    index_path = os.path.join(output_dir, 'index.html')
    try:
        with open(index_path, 'w', encoding='utf-8') as f:
            f.write(html_content)
        print(f"Index page created: {index_path}")
    except Exception as e:
        print(f"Error writing index page: {e}", file=sys.stderr)


def main():
    """Main execution function."""
    args = parse_arguments()
    
    # Verify output directory exists
    if not os.path.isdir(args.output):
        print(f"Error: Output directory does not exist: {args.output}", file=sys.stderr)
        sys.exit(1)
    
    # Load acronyms
    acronyms = load_acronyms(args.acronyms)
    print(f"Loaded {len(acronyms)} acronym mappings")
    
    # Process input file
    entries = []
    
    try:
        with open(args.input, 'r', encoding='utf-8') as f:
            for line_num, line in enumerate(f, 1):
                line = line.strip()
                if not line:
                    continue
                
                # Parse CSV line (AUid, URL)
                parts = line.split(', ', 1)
                if len(parts) != 2:
                    print(f"Warning: Skipping malformed line {line_num}", file=sys.stderr)
                    continue
                
                auid, start_url = parts
                
                # Extract collection and year from AUid
                collection_id, year = parse_auid(auid)
                
                if not collection_id or not year:
                    print(f"Warning: Could not parse collection/year from AUid on line {line_num}", file=sys.stderr)
                    continue
                
                # Get full name from acronyms
                collection_name = acronyms.get(collection_id, collection_id)
                
                print(f"Processing {collection_id} {year}...")
                
                # Fetch XML
                xml_content = fetch_xml(start_url)
                
                if xml_content is None:
                    # Record failed entry
                    entries.append({
                        'name': collection_name,
                        'year': year,
                        'filename': f"{collection_id}_{year}.html",
                        'success': False
                    })
                    continue
                
                # Parse URLs from sitemap
                urls = parse_sitemap_urls(xml_content)
                print(f"  Found {len(urls)} URLs")
                
                # Generate HTML page
                output_filename = f"{collection_id}_{year}.html"
                output_path = os.path.join(args.output, output_filename)
                
                success = generate_html_page(
                    collection_name, year, urls, args.server, auid, output_path
                )
                
                if success:
                    print(f"  Created {output_filename}")
                
                # Record entry for index
                entries.append({
                    'name': collection_name,
                    'year': year,
                    'filename': output_filename,
                    'success': success
                })
    
    except Exception as e:
        print(f"Error reading input file: {e}", file=sys.stderr)
        sys.exit(1)
    
    # Generate index page
    if entries:
        generate_index_page(entries, args.output)
    
    print(f"\nProcessing complete. Generated {len(entries)} entries.")


if __name__ == '__main__':
    main()