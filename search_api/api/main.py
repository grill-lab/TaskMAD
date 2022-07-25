import json
import sys

from flask import Flask, request
from pyserini.search import SimpleSearcher
from flask_cors import CORS
from waitress import serve

from utils import clean_results
from utils import get_sections
from utils import format_results

######## START CONFIGURATIONS ########

app = Flask(__name__)
cors = CORS(app, resources={r"*": {"origins": [""]}})

DEPLOYMENT_HOST = '0.0.0.0'
DEPLOYMENT_PORT = 5050

indexes_folders = {
    'wikipedia': 'data/wiki_index',
    'seriouseats': 'data/serious_eats_index',
    'seriouseats_recipes': 'data/serious_eats_recipes_index',
    'stack_exchange_cooking': 'data/stack_exchange_cooking_index'
}

# Searcher default parameters
NUMBER_DOCUMENTS_TO_RETRIEVE = 5
SEARCHER_TITLE_WEIGHT = 0.5
SEARCHER_CONTENTS_WEIGHT = 1

######## END CONFIGURATIONS ########

######## START DEFAULT API ENDPOINTS ########


@app.route('/search', methods=['GET'])
def retrieveSearchDocuments():
    """
        Default endpoint used in order to perform a query on the specified 
        knowledge source.
        The endpoints requires the following parameters provided as json when 
        performing the request:
        INPUT 
        - query
        - knowledge_source: The specific index key where we want to execute
        the query (as specified in the dictionary indexes_folders) 
        - number_documents_to_retrieve: Number of documents we want to retrieve. 
        
        OUTPUT
        - A dictionary containing a list of documents in Anserini format
    """

    results = {'documents': [], 'errors': []}
    try:
        query = dict(request.json)['query']
        knowledge_source = dict(request.json)['knowledge_source']
        number_documents_to_retrieve = NUMBER_DOCUMENTS_TO_RETRIEVE
        if ("number_documents_to_retrieve" in dict(request.json)):
            temp_number_documents_to_retrieve = int(
                dict(request.json)["number_documents_to_retrieve"])
            if (temp_number_documents_to_retrieve >= 1):
                number_documents_to_retrieve = temp_number_documents_to_retrieve

        # Search from the build index
        searcher = SimpleSearcher(indexes_folders[knowledge_source])

        hits = searcher.search(
            query,
            k=number_documents_to_retrieve,
            fields={
                'title':
                SEARCHER_TITLE_WEIGHT,  # negatively boost title to prioritise content search
                'contents': SEARCHER_CONTENTS_WEIGHT
            })

        for i in range(len(hits[:number_documents_to_retrieve])):
            results['documents'].append(clean_results(hits[i].raw))

    except Exception as e:
        results['errors'].append('An error occurred: ' + str(e))
        print(e)

    return results


@app.route('/extract_page', methods=['GET'])
def getIndexedPage():
    """
        Default endpoint used in order to extract a full page. 
        The endpoints requires the following parameters provided as json when 
        performing the request:
        INPUT 
        - section_id: a specific passage id. This will retireve the full page
          this passage belongs to.
        - knowledge_source: The specific index key as specified in the dictionary
          indexes_folders
        - page_id: The page id this passage belongs to. 
        
        OUTPUT
        - A dictionary containing a list of documents in Anserini format
    """
    results = {'documents': [], 'errors': []}
    try:
        section_id = dict(request.json)['section_id']
        knowledge_source = dict(request.json)['knowledge_source']
        # The index we use is based on the parameters passed
        searcher = SimpleSearcher(indexes_folders[knowledge_source])
        initial_section = searcher.doc(str(section_id)).raw()
        page_id = clean_results(initial_section)["page_id"]
        # get all section before and after within page
        sections_before = get_sections(True, searcher, int(section_id),
                                       page_id)
        sections_after = get_sections(False, searcher, int(section_id),
                                      page_id)

        # Need to revert the before list as we want the results to be in
        # the correct order
        for section in sections_before[::-1]:
            results['documents'].append(clean_results(section))
        results['documents'].append(clean_results(initial_section))
        for section in sections_after:
            results['documents'].append(clean_results(section))

        # Format the results in order to have a structured json file
        results['documents'] = format_results(results['documents'])

    except Exception as e:
        results['errors'].append('An error occurred: ' + str(e))
        print(e)
    return results


######## END DEFAULT API ENDPOINTS ########
######## START CUSTOM API ENDPOINTS ########


@app.route('/doc_by_id', methods=['GET'])
def getRecipeById():
    """
        Custom endpoint used in order to extract a page containing sequential information (steps). 
        The endpoints requires the following parameters provided as json when 
        performing the request:
        INPUT 
        - doc_id: The anserini id of the document we want to retrieve
        - knowledge_source: The specific index key as specified in the dictionary
          indexes_folders
        
        OUTPUT
        - A single sequential document or a list of errors
    """
    results = {'document': {}, 'errors': []}
    try:
        doc_id = dict(request.json)['doc_id']
        knowledge_source = dict(request.json)['knowledge_source']

        searcher = SimpleSearcher(indexes_folders[knowledge_source])
        doc_obj = searcher.doc(str(doc_id)).raw()

        if doc_obj != None:
            results['document'] = json.loads(doc_obj)

    except Exception as e:
        results['errors'].append('An error occurred: ' + str(e))
        print(e)

    return results


######## END CUSTOM API ENDPOINTS ########

if __name__ == '__main__':

    args = sys.argv
    if (len(args) == 2 and args[1].lower() == 'debug'):
        app.run(debug=True, host=DEPLOYMENT_HOST, port=DEPLOYMENT_PORT)
    elif (len(args) == 2 and args[1].lower() == 'deploy'):
        serve(app, host=DEPLOYMENT_HOST, port=DEPLOYMENT_PORT)
    else:
        print('Specify a valid deployment option (debug, deploy)')
