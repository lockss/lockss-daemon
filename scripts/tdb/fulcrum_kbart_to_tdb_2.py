#!/usr/bin/env python3
import requests
from bs4 import BeautifulSoup
import time
import csv
from urllib.parse import urlparse
import argparse

def resolve_doi_to_info(doi):
    """Resolve DOI and fetch the Fulcrum URL."""
    # Construct the CrossRef DOI URL to fetch metadata.
    base_url = 'https://api.crossref.org/works/'
    doi_url = f'{base_url}{doi}'

    try:
        # Get DOI metadata
        response = requests.get(doi_url)
        response.raise_for_status()  # Raise an error for bad responses
        data = response.json()

        # Extract the necessary information from the response
        primary_url = data['message'].get('URL', None)

        # Fetch Fulcrum URL based on DOI
        fulcrum_url = fetch_fulcrum_url(doi)

        return {
            'doi': doi,
            'url': primary_url,  # Primary URL from CrossRef
            'fulcrum_url': fulcrum_url  # The Fulcrum URL fetched
        }

    except requests.exceptions.RequestException as e:
        print(f"Error resolving DOI {doi}: {e}")
        return None

def fetch_fulcrum_url(doi):
    """Fetch the Fulcrum URL for the given DOI."""
    # Construct the DOI URL
    base_url = 'https://doi.org/'
    doi_url = f'{base_url}{doi}'

    # Send a GET request to the DOI page
    response = requests.get(doi_url)

    # Check if the request was successful
    if response.status_code == 200:
        # Parse the HTML content
        soup = BeautifulSoup(response.content, 'html.parser')

        # Extract all links
        links = soup.find_all('a', href=True)

        # Check each link to find the first one that matches the desired prefix
        for link in links:
            href = link['href']
            if href.startswith("https://www.fulcrum.org/"):
                return href  # Return the first matching URL

    else:
        print(f"Failed to fetch the DOI page. Status code: {response.status_code}")

    return None  # Return None if no matching URL is found

def resolve_csv_to_info(file_path):
    doi_column = 'title_id'
    year_column = 'date_monograph_published_print'
    title_column = 'publication_title'
    isbn_column = 'print_identifier'
    eisbn_column = 'online_identifier'

    dois_info = []
    
    with open(file_path, 'r') as file:
        csv_reader = csv.DictReader(file)
        for row in csv_reader:
            doi = row.get(doi_column, '').strip()
            if doi:
                info = resolve_doi_to_info(doi)
                if info:
                    info['csv_doi'] = doi
                    info['csv_publication_year'] = row.get(year_column, '').strip()
                    info['csv_title'] = row.get(title_column, '').strip()
                    info['csv_isbn'] = row.get(isbn_column, '').strip()
                    info['csv_eisbn'] = row.get(eisbn_column, '').strip()
                    dois_info.append(info)
                    time.sleep(0.1)  # Sleep for 0.1 seconds between requests

    return dois_info

if __name__ == "__main__":
    # Set up command-line argument parsing
    parser = argparse.ArgumentParser(description='Resolve DOIs to URLs and extract information from a CSV file.')
    parser.add_argument('input_csv', help='Path to the input CSV file')
    args = parser.parse_args()

    # Call the resolve_csv_to_info function with the input file name
    input_csv_path = args.input_csv
    dois_info = resolve_csv_to_info(input_csv_path)

    print("TDB FILE: status ; status2 ; year; isbn ; eisbn ; url_path ; title ; ")
    for info in dois_info:
        print(f"    au < exists ; exists ; {info['csv_publication_year']} ; {info['csv_isbn']} ; {info['csv_eisbn']} ; {info['fulcrum_url']} ; {info['csv_title']} ; >")