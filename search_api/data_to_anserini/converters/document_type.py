from abc import ABC, abstractmethod
from enum import Enum


class DocumentType(Enum):
    TEXT = 1,
    IMAGE = 2,
    VIDEO = 3,
    AUDIO = 4

    def __str__(self):
        return self.name.lower()


class DocumentTypeManagerAbstract(ABC):

    @abstractmethod
    def _is_document_image(self, document_string: str) -> bool:
        pass

    @abstractmethod
    def _is_document_video(self, document_string: str) -> bool:
        pass

    @abstractmethod
    def _is_document_audio(self, document_string: str) -> bool:
        pass

    def get_document_type(self, document_string: str) -> DocumentType:
        if (self._is_document_image(document_string)):
            return DocumentType.IMAGE
        elif (self._is_document_video(document_string)):
            return DocumentType.VIDEO
        elif (self._is_document_audio(document_string)):
            return DocumentType.AUDIO
        else:
            return DocumentType.TEXT


class CustomDocumentTypeManager(DocumentTypeManagerAbstract):

    def _is_document_image(self, document_string: str) -> bool:
        document_string = document_string.lower()
        if ((document_string.startswith("http://")
             or document_string.startswith("https://"))
                and (document_string.endswith(".png")
                     or document_string.endswith(".jpg")
                     or document_string.endswith(".jpeg"))):
            return True
        else:
            return False

    def _is_document_video(self, document_string: str) -> bool:
        document_string = document_string.lower()

        if ("video_separator" in document_string):
            return True
        else:
            return False

    def _is_document_audio(self, document_string: str) -> bool:
        document_string = document_string.lower()
        if ((document_string.startswith("http://")
             or document_string.startswith("https://"))
                and (document_string.endswith(".mp3")
                     or document_string.endswith(".mp4"))):
            return True
        else:
            return False
