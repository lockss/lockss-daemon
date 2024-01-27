#!/usr/bin/env python3
import csv
import requests
import argparse
import time
import logging

#Suggested input: python3 ./scripts/tdb/fulcrum_cvs_to_tdb.py InputFile.csv 5

# Set the logging level to DEBUG for development and debugging
logging.basicConfig(level=logging.INFO)

def resolve_redirects(url):
    try:
        logging.debug("Before request: %s", url)
        headers = {'User-Agent': 'LOCKSS cache'}
        response = requests.get(url, allow_redirects=True, headers=headers, timeout=10)
        logging.debug("After request: %s", url)
        response.raise_for_status()
        logging.debug(f"Resolved URL: {response.url}")
        return response.url
        
    except requests.exceptions.HTTPError as errh:
        logging.error("HTTP Error for URL %s: %s", url, errh)
    except requests.exceptions.ConnectionError as errc:
        logging.error("Error Connecting for URL %s: %s", url, errc)
    except requests.exceptions.Timeout as errt:
        logging.error("Timeout Error for URL %s: %s", url, errt)
    except requests.exceptions.RequestException as err:
        logging.error("Error resolving redirects for URL %s: %s", url, err)
    return None

def update_csv_with_redirected_urls(input_csv, url_column):
    output_csv = input_csv.replace('.csv', '_with_redirected_urls.csv')

    with open(input_csv, 'r', encoding='utf-8-sig') as infile, open(output_csv, 'w', newline='') as outfile:
        reader = csv.reader(infile)
        header = next(reader)  # Read the header
        header.append('Redirected_URL')  # Add a new column header for the redirected URL

        writer = csv.writer(outfile)
        writer.writerow(header)  # Write the updated header to the new CSV file

        for row in reader:
            url = row[url_column]
            #redirected_url = resolve_redirects(url)
            #time.sleep(1)  # Sleep for 1 second between requests (adjust as needed)

            # Append the redirected URL to the row
            #row.append(redirected_url if redirected_url else 'Failed to Resolve Redirects')
            #writer.writerow(row)

            # Exception handling inside the loop
            try:
                #url = row[url_column]
                redirected_url = resolve_redirects(url)
                time.sleep(1)  # Sleep for 1 second between requests (adjust as needed)

                # Append the redirected URL to the row
                row.append(redirected_url if redirected_url else 'Failed to Resolve Redirects')
            except UnicodeDecodeError as e:
                logging.error("UnicodeDecodeError: %s", e)
                # Handle the error (e.g., log it) and continue with the next line
                row.append('Failed to Resolve Redirects')

            writer.writerow(row)

    logging.info(f"CSV file with redirected URLs saved to: {output_csv}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Resolve redirects in a CSV file.')
    parser.add_argument('input_csv', help='Path to the input CSV file')
    parser.add_argument('url_column', type=int, help='Column number containing URLs (0-based index)')
    args = parser.parse_args()

    update_csv_with_redirected_urls(args.input_csv, args.url_column)
