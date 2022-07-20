import json
import re

from converters.anserini_converter import AnseriniConverterAbstract
from converters.document_type import CustomDocumentTypeManager


class StackExchangeCookingConverter(AnseriniConverterAbstract):
    __documentType: CustomDocumentTypeManager

    def __init__(self) -> None:
        super().__init__()
        self.__documentType: CustomDocumentTypeManager = CustomDocumentTypeManager(
        )

    def convert_and_write_data_to_anserini_json(self, input_file: str,
                                                output_file: str) -> None:

        paragraph_id = 0
        with open(input_file, 'r') as json_file:
            for line in json_file:
                line = line.strip()
                document = json.loads(line)

                # Write the main post
                paragraph_id = self.__write_main_post(document, paragraph_id,
                                                      output_file)

                # Need to repeat the same process for each other children of the current document.
                if 'Children' in document:
                    for answerId, child in enumerate(document['Children']):
                        paragraph_id = self.__write_subpost(
                            child, document['Title'], paragraph_id,
                            output_file, answerId + 1)

    def __write_main_post(self, post_obj, paragraph_id, output_file):
        if 'Title' not in post_obj:
            return paragraph_id
        with open(output_file, "a+") as json_file_write:
            # Split based on images
            matches = re.split('<img src="(.*?)">', post_obj["Body"])
            for match in matches:
                # If the text is an image we need to extract only the image
                if match.startswith("http"):
                    image_url = match.split('" ')[0]

                    # Save the image
                    json_line = json.dumps({
                        "id":
                        f"{paragraph_id}",
                        "hashed_id":
                        f"{self._hashString(str(paragraph_id) + post_obj['Title'] + image_url)}",
                        "page_id":
                        f"{self._hashString(post_obj['Title'])}",
                        "page_title":
                        f"{post_obj['Title']}",
                        "section_title":
                        f"{post_obj['Title']}",
                        "contents":
                        f"{image_url}",
                        "sentences": [image_url],
                        "document_type":
                        str(self.__documentType.get_document_type(image_url)),
                        "source_id":
                        f"{post_obj['Id']}",
                        "source_url":
                        f"https://cooking.stackexchange.com/questions/{post_obj['Id']}"
                    })
                    json_file_write.write(str(json_line) + "\n")
                    paragraph_id += 1
                else:
                    match = re.sub(re.compile('<.*?>'), '', match)
                    inner_sentences = self._split_paragraph_into_sentences(
                        match)
                    # Save the paragraph

                    json_line = json.dumps({
                        "id":
                        f"{paragraph_id}",
                        "hashed_id":
                        f"{self._hashString(str(paragraph_id) + post_obj['Title'] + match)}",
                        "page_id":
                        f"{self._hashString(post_obj['Title'])}",
                        "page_title":
                        f"{post_obj['Title']}",
                        "section_title":
                        f"{post_obj['Title']}",
                        "contents":
                        f"{match}",
                        "sentences":
                        inner_sentences,
                        "document_type":
                        str(self.__documentType.get_document_type(match)),
                        "source_id":
                        f"{post_obj['Id']}",
                        "source_url":
                        f"https://cooking.stackexchange.com/questions/{post_obj['Id']}"
                    })
                    json_file_write.write(str(json_line) + "\n")
                    paragraph_id += 1

            # Need to write lines for all the comments associated to this post
            if "Comments" in post_obj:
                paragraph_id = self.__write_comments(post_obj,
                                                     post_obj['Title'],
                                                     post_obj['Id'],
                                                     paragraph_id, output_file)

        return paragraph_id

    def __write_comments(self, post_obj, page_title, page_id, paragraph_id,
                         output_file):
        comments = post_obj['Comments']
        with open(output_file, 'a+') as json_file_write:
            for comment in comments:
                # Split comments into sentences
                sentences = self._split_paragraph_into_sentences(
                    comment['TextCleaned'])
                json_line = json.dumps({
                    "id":
                    f"{paragraph_id}",
                    "hashed_id":
                    f"{self._hashString(str(paragraph_id) + comment['Id'] + comment['Text'] + comment['CreationDateTimestamp'])}",
                    "page_id":
                    f"{self._hashString(page_title)}",
                    "page_title":
                    f"{page_title}",
                    "section_title":
                    f"Comments to: {post_obj['Title']}",
                    "contents":
                    f"{comment['TextCleaned']}",
                    "sentences":
                    sentences,
                    "document_type":
                    str(
                        self.__documentType.get_document_type(
                            comment['TextCleaned'])),
                    "source_id":
                    f"{page_id}",
                    "source_url":
                    f"https://cooking.stackexchange.com/questions/{page_id}"
                })
                json_file_write.write(str(json_line) + "\n")
                paragraph_id += 1

        return paragraph_id

    def __write_subpost(self,
                        post_obj,
                        page_title,
                        paragraph_id,
                        output_file,
                        answerId=1):
        with open(output_file, "a+") as json_file_write:
            post_obj['Title'] = f"Answer {answerId}"
            # Split based on images
            matches = re.split('<img src="(.*?)">', post_obj["Body"])
            for match in matches:
                # If the text is an image we need to extract only the image
                if match.startswith("http"):
                    image_url = match.split('" ')[0]

                    # Save the image
                    json_line = json.dumps({
                        "id":
                        f"{paragraph_id}",
                        "hashed_id":
                        f"{self._hashString(str(paragraph_id) + post_obj['Title'] + image_url)}",
                        "page_id":
                        f"{self._hashString(page_title)}",
                        "page_title":
                        f"{page_title}",
                        "section_title":
                        f"{post_obj['Title']}",
                        "contents":
                        f"{image_url}",
                        "sentences": [image_url],
                        "document_type":
                        str(self.__documentType.get_document_type(image_url)),
                        "source_id":
                        f"{post_obj['ParentId']}",
                        "source_url":
                        f"https://cooking.stackexchange.com/questions/{post_obj['ParentId']}"
                    })
                    json_file_write.write(str(json_line) + "\n")
                    paragraph_id += 1
                else:
                    match = re.sub(re.compile('<.*?>'), '', match)
                    inner_sentences = self._split_paragraph_into_sentences(
                        match)
                    # Save the paragraph

                    json_line = json.dumps({
                        "id":
                        f"{paragraph_id}",
                        "hashed_id":
                        f"{self._hashString(str(paragraph_id) + post_obj['Title'] + match)}",
                        "page_id":
                        f"{self._hashString(page_title)}",
                        "page_title":
                        f"{page_title}",
                        "section_title":
                        f"{post_obj['Title']}",
                        "contents":
                        f"{match}",
                        "sentences":
                        inner_sentences,
                        "document_type":
                        str(self.__documentType.get_document_type(match)),
                        "source_id":
                        f"{post_obj['ParentId']}",
                        "source_url":
                        f"https://cooking.stackexchange.com/questions/{post_obj['ParentId']}"
                    })
                    json_file_write.write(str(json_line) + "\n")
                    paragraph_id += 1

            # Need to write lines for all the comments associated to this post
            if "Comments" in post_obj:
                paragraph_id = self.__write_comments(post_obj, page_title,
                                                     post_obj['ParentId'],
                                                     paragraph_id, output_file)
        return paragraph_id