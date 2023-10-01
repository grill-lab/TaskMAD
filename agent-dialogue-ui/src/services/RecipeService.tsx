import { Struct } from "google-protobuf/google/protobuf/struct_pb";
import { ADConnection } from "../common/ADConnection";
import { RecipeModel } from "../models/RecipeModel";

// Class used in order to manage the retrieval of topics
export class RecipeService {
    public static getAllRecipes = async (): Promise<object[]> => {
        const requestOptions = {
            method: 'GET',
            // headers: { 'Content-Type': 'application/json', 'Origin': '*' },
            headers: { 'Origin': '*' },
        };

        var dataJson = undefined;
        try {
            const topics_url = process.env.REACT_APP_TOPIC_URL;
            const response = await fetch(topics_url as string, requestOptions);
            dataJson = await response.json();

            if (dataJson !== undefined) {
                // TODO update this to "topics"
                return dataJson['recipes'];
            }

        } catch (error) {
            console.log('Recipe fetch error: %o', error);
        }

        return dataJson;
    }

    public static getTopicById = async (topicId: string): Promise<RecipeModel | undefined> => {
        const requestOptions = {
            method: 'GET',
            // headers: { 'Content-Type': 'application/json', 'Origin': '*' },
            headers: { 'Origin': '*' },
        };

        var dataJson = undefined;
        try {
            const topics_url = process.env.REACT_APP_DATA_URL;
            const response = await fetch(topics_url as string, requestOptions);
            dataJson = await response.json();

            if (dataJson !== undefined && topicId in dataJson) {
                console.log('Topic data: %o', dataJson[topicId]);
                return RecipeModel.jsonToRecipeModel(dataJson[topicId]['document']);
            }

        } catch (error) {
            console.log('Recipe fetch error: %o', error);
        }

        return dataJson;
    }

}
