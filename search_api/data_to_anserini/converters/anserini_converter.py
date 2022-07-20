from typing import List, Optional
from spacy.lang.en import English
import hashlib
from abc import ABC, abstractmethod


class AnseriniConverterAbstract(ABC):

    __nlp: English

    def __init__(self) -> None:
        super().__init__()
        self.__nlp = English()
        self.__nlp.add_pipe('sentencizer')

    """
        Abstract method used in order to convert data to anserini format and write it 
        down to file. 
    """

    @abstractmethod
    def convert_and_write_data_to_anserini_json(self, input_file: str,
                                                output_file: str) -> None:
        pass

    """
        Splits a paragraph into multiple sentences
    """

    def _split_paragraph_into_sentences(self, paragraph: str) -> List[str]:
        doc = self.__nlp(paragraph)
        return [sent.text.strip() for sent in doc.sents]

    """
        Hashes a string and returns half MD5 string.
    """

    def _hashString(self, s: str) -> str:
        md5Hash: str = hashlib.md5(s.encode('utf-8')).hexdigest()
        return md5Hash[:len(md5Hash) // 2]