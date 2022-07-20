import json

from converters.anserini_converter import AnseriniConverterAbstract
from converters.document_type import CustomDocumentTypeManager


class WikipediaConverter(AnseriniConverterAbstract):

    __documentType: CustomDocumentTypeManager

    def __init__(self) -> None:
        super().__init__()
        self.__documentType: CustomDocumentTypeManager = CustomDocumentTypeManager(
        )

    def convert_and_write_data_to_anserini_json(self, input_file: str,
                                                output_file: str) -> None:

        current_paragraph_num: int = 0

        with open(output_file, "w+") as json_file:
            with open(input_file) as wikidata:
                for page in wikidata:
                    page = page.strip()
                    page = json.loads(page)
                    try:
                        wikipedia_page_id = page['wikipedia_id']
                        wikipedia_page_title = page['wikipedia_title']
                        wikipedia_page = page['text']
                        wikipedia_page_joined = " ".join(wikipedia_page)
                        current_section_title = page['wikipedia_title']

                        for paragraph in wikipedia_page:
                            paragraph = paragraph.replace('\n', '').replace(
                                '\t', '')
                            # Remove the markup text
                            if paragraph.startswith('Section::::'):
                                paragraph = paragraph.replace(
                                    'Section::::', '')
                                # As the section change we keep track of it
                                # as we want to make sure we know to which section
                                # each paragraphs belongs to
                                current_section_title = paragraph
                            if paragraph.startswith('BULLET::::'):
                                paragraph = paragraph.replace('BULLET::::', '')
                            if paragraph.startswith('NUMBER::::'):
                                paragraph = paragraph.replace('NUMBER::::', '')

                            sentences = self._split_paragraph_into_sentences(
                                paragraph)

                            json_line = json.dumps({
                                "id":
                                f"{current_paragraph_num}",
                                "hashed_id":
                                f"{self._hashString(str(current_paragraph_num) + paragraph)}",
                                "page_id":
                                f"{self._hashString(wikipedia_page_joined)}",
                                "page_title":
                                f"{wikipedia_page_title}",
                                "section_title":
                                f"{current_section_title}",
                                "contents":
                                f"{paragraph}",
                                "sentences":
                                sentences,
                                "document_type":
                                str(
                                    self.__documentType.get_document_type(
                                        paragraph)),
                                "source_id":
                                f"{wikipedia_page_id}",
                                "source_url":
                                f"https://en.wikipedia.org/wiki/{wikipedia_page_title.replace(' ', '_')}",
                            })
                            json_file.write(str(json_line) + "\n")
                            current_paragraph_num += 1
                    except Exception as e:
                        print(e)
                        continue