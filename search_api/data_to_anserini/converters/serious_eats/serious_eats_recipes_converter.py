import json
from typing import List

from converters.anserini_converter import AnseriniConverterAbstract
from converters.document_type import CustomDocumentTypeManager


class SeriousEatsRecipesConverter(AnseriniConverterAbstract):

    __documentType: CustomDocumentTypeManager

    def __init__(self) -> None:
        super().__init__()
        self.__documentType: CustomDocumentTypeManager = CustomDocumentTypeManager(
        )

    def convert_and_write_data_to_anserini_json(self, input_file: str,
                                                output_file: str) -> None:

        with open(output_file, "w+") as json_file_write:
            paragraph_id: int = 0
            with open(input_file, 'r') as json_file:
                for line in json_file:
                    line = line.strip()
                    document = json.loads(line)
                    sentences: List[
                        str] = self._split_paragraph_into_sentences(
                            document['description'])
                    step_sentences: List[List[str]] = [
                        self._split_paragraph_into_sentences(step)
                        for step in document['steps']
                    ]
                    json_line: str = json.dumps({
                        "id":
                        f"{paragraph_id}",
                        "hashed_id":
                        f"{self._hashString(str(paragraph_id) + document['description'])}",
                        "page_id":
                        f"{self._hashString(document['source_url'])}",
                        "page_title":
                        f"{document['title']}",
                        "section_title":
                        f"{document['title']}",
                        "contents":
                        f"{document['description']}",
                        "sentences":
                        sentences,
                        "document_type":
                        str(
                            self.__documentType.get_document_type(
                                document['description'])),
                        "source_id":
                        f"{document['doc_id']}",
                        "source_url":
                        f"{document['source_url']}",
                        "required_equipment":
                        document['required_equipment'],
                        "required_ingredient":
                        document['required_ingredient'],
                        "duration_minutes_prep":
                        f"{document['duration_minutes_prep']}",
                        "duration_minutes_cooking":
                        f"{document['duration_minutes_cooking']}",
                        "duration_minutes_total":
                        f"{document['duration_minutes_total']}",
                        "makes_and_serves":
                        document['makes_and_serves'],
                        "steps":
                        document['steps'],
                        "steps_images":
                        document['steps_images'],
                        "steps_sentences":
                        step_sentences,
                        "all_images":
                        document['all_images'],
                    })
                    json_file_write.write(str(json_line) + "\n")
                    paragraph_id += 1
