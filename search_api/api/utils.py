import json


def clean_results(result):
    result = json.loads(result)
    return result


def get_sections(before, searcher, section_id, page_id):
    curr_sec_id = section_id
    curr_page_id = page_id
    sections = []
    limit_handler = 0

    while curr_page_id == page_id:
        if before:
            curr_sec_id -= 1
        else:
            curr_sec_id += 1
        current_section = searcher.doc(str(curr_sec_id))

        # As there is no way for us to know when we reach the last element
        # indexes, we need to stop when n iterations with empty results are returned.
        # In this way we avoid getting stuck in an infinite loop.
        if current_section is None:
            limit_handler += 1
            if limit_handler > 10000:
                break
            continue
        else:
            limit_handler = 0
        curr_page_id = clean_results(current_section.raw())["page_id"]

        if curr_page_id == page_id:
            sections.append(current_section.raw())

    return sections


def format_results(results_list):
    results_formatted = {
        'page_id': '',
        'page_title': '',
        'source_url': '',
        'sections': []
    }

    isFirstIter = True
    current_section = ''
    current_section_dict = {}
    for result in results_list:
        # If we are in the first iteration, we set the general
        # page configurations
        if isFirstIter:
            results_formatted['page_id'] = result['page_id']
            results_formatted['page_title'] = result['page_title']
            results_formatted['source_url'] = result['source_url']
            current_section = result['section_title']
            isFirstIter = False

        # If the current section changes it means that we are in a new section
        # Hence, we save the current_section_dict that we already processed
        # for the previous paragraphs and we reset the values to default
        if current_section != result['section_title']:
            current_section = result['section_title']
            results_formatted['sections'].append(current_section_dict)
            current_section_dict = {}

        # If current_section not in current_section_dict then it means that we are
        # about to start to process this sessions. hence we create the current_section_dict
        # dictionary
        if (current_section not in current_section_dict):
            current_section_dict[current_section] = [{
                "id":
                result["id"],
                "contents":
                result["contents"],
                "hashed_id":
                result["hashed_id"],
                "sentences":
                result["sentences"]
            }]
        # Otherwise we keep appending new processed paragraphs
        else:
            current_section_dict[current_section].append({
                "id":
                result["id"],
                "contents":
                result["contents"],
                "hashed_id":
                result["hashed_id"],
                "sentences":
                result["sentences"]
            })

    # We need to append the last document extracted
    results_formatted['sections'].append(current_section_dict)

    # Laslty, if the page had only one section we need to add it
    # as with the current implementation it would have not been added.
    if (len(results_formatted['sections']) == 0):
        results_formatted['sections'].append(current_section_dict)

    return results_formatted