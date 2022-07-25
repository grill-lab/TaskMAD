from typing import List
from spacy.lang.en import English
import hashlib
from abc import ABC, abstractmethod


class AnseriniConverterAbstract(ABC):

    __nlp: English

    def __init__(self) -> None:
        super().__init__()
        self.__nlp = English()
        self.__nlp.add_pipe('sentencizer')

    @abstractmethod
    def convert_and_write_data_to_anserini_json(self, input_file: str,
                                                output_file: str) -> None:
        """
            Abstract method used in order to convert data to anserini format and write it 
            down to file. 
        """
        pass

    def _split_paragraph_into_sentences(self, paragraph: str) -> List[str]:
        """
            Splits a paragraph into multiple sentences
        """
        doc = self.__nlp(paragraph)
        return [sent.text.strip() for sent in doc.sents]

    def _hashString(self, s: str) -> str:
        """
            Hashes a string and returns half MD5 string.
        """
        md5Hash: str = hashlib.md5(s.encode('utf-8')).hexdigest()
        return md5Hash[:len(md5Hash) // 2]