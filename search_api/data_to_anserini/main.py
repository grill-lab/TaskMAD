import time

from converters.serious_eats.serious_eats_recipes_converter import SeriousEatsRecipesConverter
from converters.serious_eats.serious_eats_converter import SeriousEatsConverter
from converters.wikipedia.wikipedia_converter import WikipediaConverter
from converters.stack_exchange.stack_exchange_cooking_converter import StackExchangeCookingConverter

if __name__ == '__main__':

    ###### DEFINING CONVERTERS ######
    seriousEatsToAnseriniConverter: SeriousEatsConverter = SeriousEatsConverter(
    )
    seriousEatsRecipesToAnseriniConverter: SeriousEatsRecipesConverter = SeriousEatsRecipesConverter(
    )
    wikipediaToAnseriniConverter: WikipediaConverter = WikipediaConverter()
    stackExchangeToAnseriniConverter: StackExchangeCookingConverter = StackExchangeCookingConverter(
    )

    try:
        print('Starting to convert...')
        start = time.time()

        ###### START CONVERTERS ######
        # seriousEatsToAnseriniConverter.convert_and_write_data_to_anserini_json(
        #     input_file="", output_file="")

        # seriousEatsRecipesToAnseriniConverter.convert_and_write_data_to_anserini_json(
        #     input_file="", output_file="")

        wikipediaToAnseriniConverter.convert_and_write_data_to_anserini_json(
            input_file="", output_file="")

        # stackExchangeToAnseriniConverter.convert_and_write_data_to_anserini_json(
        #     input_file=
        #     "/Users/alessandrospeggiorin/Downloads/stack_exchange_cooking_data.json",
        #     output_file="test.jsonl")

        ###### END CONVERTERS ######

        end = time.time()
        print(f'Conversion completed in {end-start:.2f} seconds.')
    except Exception as e:
        print(e)
