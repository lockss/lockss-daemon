#!/usr/bin/env python3
import requests
import time
import csv
from urllib.parse import urlparse
import argparse

def resolve_doi_to_info(doi):
    base_url = 'https://api.crossref.org/works/'
    url = f'{base_url}{doi}'
    
    try:
        response = requests.get(url)
        response.raise_for_status()  # Raise an HTTPError for bad responses (4xx or 5xx)
        data = response.json()
        
#        # Extract the URL from the primary resource
#        if 'URL' in data['message'].get('resource', {}).get('primary', {}):
#            return data['message']['resource']['primary']['URL']
#        else:
#            return None  # If URL is not available

        # Extract the URL from the primary resource
        url = data['message'].get('resource', {}).get('primary', {}).get('URL', None)
        path = extract_path_from_url(url)

        info = {
            'doi': doi,
            'url': path
        }

        return info

    except requests.exceptions.RequestException as e:
        print(f"Error resolving DOI {doi}: {e}")
        return None

def extract_path_from_url(url):
    parsed_url = urlparse(url)
    path = parsed_url.path
    # Remove the leading forward slash
    path_without_slash = path[1:] if path.startswith('/') else path
    return path_without_slash

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
                    #year = row.get(year_column, '').strip()
                    #title = row.get(title_column, '').strip()
                    #isbn = row.get(isbn_column, '').strip()
                    #eisbn = row.get(eisbn_column, '').strip()
                    info['csv_publication_year'] = row.get(year_column, '').strip()
                    info['csv_title'] = row.get(title_column, '').strip()
                    info['csv_isbn'] = row.get(isbn_column, '').strip()
                    info['csv_eisbn'] = row.get(eisbn_column, '').strip()
                    dois_info.append(info)
                    time.sleep(1)  # Sleep for 1 second between requests

    return dois_info

if __name__ == "__main__":
    # Set up command-line argument parsing
    parser = argparse.ArgumentParser(description='Resolve DOIs to URLs and extract information from a CSV file.')
    parser.add_argument('input_csv', help='Path to the input CSV file')
    args = parser.parse_args()
    
    # Call the resolve_csv_to_info function with the input file name
    input_csv_path = args.input_csv

    # Example usage: provide the path to the file containing DOIs
    #input_csv_path = '../SageEdits/BAR_dois_10.txt'
    #input_csv_path = '../SageEdits/bar_pre2020_collection_2023-11-16.csv'
    #doi_column_name = 'title_id'
    #year_column_name = 'date_monograph_published_print'
    #title_column_name = 'publication_title'
    #isbn_column_name = 'print_identifier'
    #eisbn_column_name = 'online_identifier'

    #dois_info = resolve_csv_to_info(input_csv_path, doi_column_name, year_column_name, title_column_name, isbn_column_name, eisbn_column_name)
    dois_info = resolve_csv_to_info(input_csv_path)

    print("TDB FILE: status ; status2 ; year; isbn ; eisbn ; url_path ; title ; ")
    for info in dois_info:
        #print(f"{info['csv_doi']} -> {info['url']} -> {info['csv_publication_year']} -> {info['csv_isbn']} -> {info['csv_eisbn']} -> {info['csv_title']}")
        print(f"    au < exists ; exists ; {info['csv_publication_year']} ; {info['csv_isbn']} ; {info['csv_eisbn']} ; {info['url']} ; {info['csv_title']} ; >")
