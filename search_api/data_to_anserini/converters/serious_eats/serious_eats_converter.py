import json
from typing import List, Optional, Tuple, Union
import re

from converters.anserini_converter import AnseriniConverterAbstract
from converters.document_type import CustomDocumentTypeManager


class SeriousEatsConverter(AnseriniConverterAbstract):

    __documentType: CustomDocumentTypeManager

    def __init__(self) -> None:
        super().__init__()
        self.__documentType: CustomDocumentTypeManager = CustomDocumentTypeManager(
        )

    """
        Function used to parse custom tagged text and return a list of 
        headings and paragraphs if present in the tagged text. 
    """

    def __parse_tagged_text(
        self,
        tagged_text: str,
        headings: List[Union[str, None]] = [],
        paragraphs: List[List[str]] = []
    ) -> Tuple[List[Union[str, None]], List[List[str]]]:
        # Find first occurence of defined tags
        par_index = tagged_text.find('<par>')
        heading_index = tagged_text.find('<head>')
        subheading_index = tagged_text.find('<sub>')
        product_index = tagged_text.find('<prod>')
        section_img_index = tagged_text.find('<section_img>')
        caption_index = tagged_text.find('<caption>')
        video_index = tagged_text.find('<video>')

        # It there are not tags return.
        if (par_index == -1 and heading_index == -1 and subheading_index == -1
                and section_img_index == -1 and video_index == -1):
            return (headings, paragraphs)

        # Check if we found a paragraph
        if (par_index == 0):
            # Extract the paragraph text
            substring = re.search("<par>(.*?)</par>",
                                  tagged_text).group(1)  # type: ignore

            # Check if the list of paragraphs is empty
            if (len(paragraphs) == 0):
                paragraphs.append([substring])
                headings.append(None)
            else:
                paragraphs[-1].append(substring)

            # Remove the processed section
            tagged_text = tagged_text.replace('<par>' + substring + '</par>',
                                              '')

        # Check if we found an image
        if (section_img_index == 0):
            # Extract the section_image text
            substring = re.search("<section_img>(.*?)</section_img>",
                                  tagged_text).group(1)  # type: ignore

            # Check if the list of paragraphs is empty
            if (len(paragraphs) == 0):
                paragraphs.append([substring])
                headings.append(None)
            else:
                paragraphs[-1].append(substring)

            # Remove the processed section
            tagged_text = tagged_text.replace(
                '<section_img>' + substring + '</section_img>', '')

        # Check if we found a video
        if (video_index == 0):
            substring = re.search("<video>(.*?)</video>",
                                  tagged_text).group(1)  # type: ignore

            # Check if the list of paragraphs is empty
            if (len(paragraphs) == 0):
                paragraphs.append([substring])
                headings.append(None)
            else:
                paragraphs[-1].append(substring)

            # Removed the processed section
            tagged_text = tagged_text.replace(
                '<video>' + substring + '</video>', '')

        # Check if we found a heading
        if (heading_index == 0):
            substring = re.search("<head>(.*?)</head>",
                                  tagged_text).group(1)  # type: ignore
            headings.append(substring)

            # When we find a heading we always append an empty list becasue from now on we
            # will store all the following paragraphs
            paragraphs.append([])
            # Removed the processed section
            tagged_text = tagged_text.replace('<head>' + substring + '</head>',
                                              '')

        # Check if we found a subheading
        if (subheading_index == 0):
            substring = re.search("<sub>(.*?)</sub>",
                                  tagged_text).group(1)  # type: ignore
            headings.append(substring)
            paragraphs.append([])
            # Removed the processed section
            tagged_text = tagged_text.replace('<sub>' + substring + '</sub>',
                                              '')

        # Here we put all the tags we need to remove
        if (caption_index == 0):
            substring = re.search("<caption>(.*?)</caption>",
                                  tagged_text).group(1)  # type: ignore
            tagged_text = tagged_text.replace(
                '<caption>' + substring + '</caption>', '')

        if (product_index == 0):
            substring = re.search("<prod>(.*?)</prod>",
                                  tagged_text).group(1)  # type: ignore
            # Removed the processed section
            tagged_text = tagged_text.replace('<prod>' + substring + '</prod>',
                                              '')

        return self.__parse_tagged_text(
            tagged_text,
            headings,
            paragraphs,
        )

    def __create_heading(self, heading: Optional[str],
                         document_title: str) -> str:
        if heading is None:
            return document_title
        else:
            return heading

    def convert_and_write_data_to_anserini_json(self, input_file: str,
                                                output_file: str) -> None:
        with open(output_file, "w+") as json_file_write:
            paragraph_id: int = 0
            with open(input_file, 'r') as json_file:
                for line in json_file:
                    line = line.strip()
                    document = json.loads(line)
                    headings, paragraphs = self.__parse_tagged_text(
                        document["text_tagged"], headings=[], paragraphs=[])
                    for index, heading in enumerate(headings):
                        heading = self.__create_heading(
                            heading, document['title'])
                        for paragraph in paragraphs[index]:
                            sentences: List[
                                str] = self._split_paragraph_into_sentences(
                                    paragraph)

                            json_line: str = json.dumps({
                                "id":
                                f"{paragraph_id}",
                                "hashed_id":
                                f"{self._hashString(str(paragraph_id) + paragraph)}",
                                "page_id":
                                f"{self._hashString(document['text'])}",
                                "page_title":
                                f"{document['title']}",
                                "section_title":
                                f"{heading}",
                                "contents":
                                f"{paragraph}",
                                "sentences":
                                sentences,
                                "document_type":
                                str(
                                    self.__documentType.get_document_type(
                                        paragraph)),
                                "source_id":
                                f"{document['doc_id']}",
                                "source_url":
                                f"{document['source_url']}",
                                "related_documents":
                                f"{document['linked_recipes']}",
                            })
                            json_file_write.write(str(json_line) + "\n")
                            paragraph_id += 1
