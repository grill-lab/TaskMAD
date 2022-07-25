# Converting Data To Anserini Format

## Overview

The first step required in order to use the **Search API** module, is to convert your own datasets into a format that can be consumed by Anserini. We provide a set of predefined functionalities to simply this process. 

## Anserini Data Format

To index and retrieve data using Anserini, we need to convert data as `JSON lines` stored in a `.jsonl` file. In this file, each line has to be a passage that can be retrieved by Anserini. 

However, independently from the datataset you'll be using, and independently from your own custom set of attributed for each passage, each `JSON line` in the output file must have the following set of attributes for the correct functioning of **TaskMAD**. 

All the compulsory attributes are listed below:

```json
{
    "id": "(string) -> Anserini ID. This must an increasing numeric integer.",
    "hashed_id": "(string) -> Unique hashed id to identify the passage",
    "page_id": "(string) -> Unique page id. All passages belonging to the same page should share the same one",
    "page_title": "(string) -> Page Title",
    "section_title": "(string) -> Section title this passage belongs to",
    "contents": "(string) -> This is the actual content that can be retrieved by Anserini. This is the passage",
    "sentences": "(List[string]) -> A list of the sentences within the passage",
    "document_type": "(string) -> specific document type. It can be text, video, image, audio.",
    "source_id": "(string) -> Source ID of the document this passage came from. This field is simply there to help map back the passage to its original datasource",
    "source_url": "(string) -> Source Url of the document this passage came from. This field is simply there to help map back the passage to its original datasource"
}
```

**It is essential to mention that the filed `id` has to be an integer of increasing value. Moreover, related and successive passages MUST have successive ids so that it will be possible to retrieve all passages belonging to a page by simply iterating with a counter.**

### Contextual Sequential Data

TaskMAD, also provides the functionality of showing contextual sequential information to to user (in the chat interface). If the Anserini file we are generating is of that type, we also needd to add the following **compulsory** attributes

```json
{
    "steps_sentences": "(List[List[string]]) -> A list of lists containing all the sentences per step (in order i.e. step at index 0 is the first one)",
    "steps_images": "(List[List[string]]) -> A list of lists containing all the urls of the images per step. Here, if there are no images per step, we still have to place an empty list []"
}
```

## Converting your dataset to Anserini Format

To simply the process of converting your own dataset to Anserini format we provide a set of functions and examples (Converters for SeriousEats, Wikipedia etc..). 

More precisely, there are 3 steps you need to take to convert your own dataset. 

### Step 1 - Create your own Converter

To create your own converter, you simply need to extend the provided Abstract class `AnseriniConverter` implemented in `anserini_converter.py`. This class provides, a set of functions to hash strings (`_hashString(str)`) and to split strings into sentences (`_split_paragraph_into_sentences(str)`). 

Moreover, the Abstract class requires you to implement the abstract method `convert_and_write_data_to_anserini_json(input_file: str, output_file: str)` in which the logic for this conversion has to be implemented. You can implement your own logic as long as in this function you generate a `.jsonl` `output_file` of the format highlighted in the previous section. 

An example is provided below: 

```python
class YourCustomConverter(AnseriniConverterAbstract):

    __documentType: YourCustomDocumentTypeManager

    def __init__(self) -> None:
        super().__init__()
        self.__documentType: YourCustomDocumentTypeManager = YourCustomDocumentTypeManager()

    def convert_and_write_data_to_anserini_json(self, input_file: str,
                                                output_file: str) -> None:
        with open(output_file, "w+") as json_file_write:
            paragraph_id: int = 0
            with open(input_file, 'r') as json_file:
                for line in json_file:
                    line = line.strip()
                    document = json.loads(line)
                    
                    json_line: str = json.dumps({
                        "id": f"{paragraph_id}",
                        "hashed_id": f"{self._hashString(...)}",
                        "page_id": f"{self._hashString(...)}",
                        "page_title": "...",
                        "section_title": "...",
                        "contents": "...",
                        "sentences": [...]
                        "document_type":
                        str(
                            self.__documentType.get_document_type(
                                paragraph)),
                        "source_id": "...",
                        "source_url": "..."
                    })
                    json_file_write.write(str(json_line) + "\n")
                    paragraph_id += 1

```

### Step 2 - Create your own Document Type

As you might have noticed, in the constructor of the custom converter we define `__documentType: YourCustomDocumentTypeManager`. 

More precisely, in the Anserini JSON we generate, we need to specify whether the content of the passage is text, an image, a video or an audio. This is due to that fact that, even if we store the content as a string, it will be easier for us to later instantly disambiguate between document types. 

We provide an example of rudimental way to automatically disambiguate between documents in the class `CustomDocumentTypeManager` in `document_type.py`. 

However, in order to implement your own way of distinguishing between document types you can simply extend the Abstract class `DocumentTypeManagerAbstract` which requires you to implement the methods `_is_document_image(str)`, `_is_document_video(str)`, `_is_document_audio()` in which you can provide your way of classifying documents. 

The type is then automatically inferred by using calling the concrete method `get_document_type(str)` which returns a `DocumentType` enum. 

An example is provided below: 

```python

class CustomDocumentTypeManager(DocumentTypeManagerAbstract):

    def _is_document_image(self, document_string: str) -> bool:
        document_string = document_string.lower()
        # if document is an image
        if (...):
            return True
        else:
            return False

    def _is_document_video(self, document_string: str) -> bool:
        document_string = document_string.lower()
        # if document is a video
        if (...):
            return True
        else:
            return False

    def _is_document_audio(self, document_string: str) -> bool:
        document_string = document_string.lower()
        # if document is an audio
        if (...):
            return True
        else:
            return False

```

Lastly, it is worth mentioning that when writing the `DocumentType` to json we need its string representation which can be easilly generated with 

```python
str(myDocumentTypeObject.get_document_type("My string"))
```

### Step 3 - Using your own converter and convert your dataset

To use your own converter you simply have to instantiate an object and call the method `convert_and_write_data_to_anserini_json(str,str)`. This has to be done in `main.py`

```python
# Instantiate Converter 


if __name__ == '__main__':

    ###### DEFINING CONVERTERS ######
    myConverter:  = myConverterObject()

    try:
        print('Starting to convert...')
        start = time.time()

        ###### START CONVERTERS ######
        myConverter.convert_and_write_data_to_anserini_json(input_file="", output_file="")
        ###### END CONVERTERS ######

        end = time.time()
        print(f'Conversion completed in {end-start:.2f} seconds.')
    except Exception as e:
        print(e)

```




