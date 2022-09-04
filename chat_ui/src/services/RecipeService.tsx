import { Struct } from "google-protobuf/google/protobuf/struct_pb";
import { ADConnection } from "../common/ADConnection";
import { RecipeModel } from "../models/RecipeModel";
import { ISequentialPageModel } from "../models/SequentialPageModel";


// Class used in order to manage the retrieval of recipes
export class RecipeService {
    public static getAllRecipes = async (): Promise<object[]> => {
        const requestOptions = {
            method: 'GET',
            headers: { 'Content-Type': 'application/json', 'Origin': '*' },
        };

        let dataJson = undefined;
        try {
            const response = await fetch('https://storage.googleapis.com/taskmad-public-bucket/associated_recipes.json', requestOptions);
            dataJson = await response.json();

            if (dataJson !== undefined) {

                return dataJson['recipes'];
            }

        } catch (error) {
            console.log(error)
        }

        return dataJson;
    }

    // Retrieve a recipe based on the id passed
    public static getRecipeById = async (recipeId: string, connection: ADConnection | undefined): Promise<ISequentialPageModel | undefined> => {

        if (connection !== undefined) {

            let apiResponse: { [key: string]: any; } | undefined = await connection?.agentInteractionApi(
                Struct.fromJavaScript({
                    "service_name": "search_api",
                    "api_endpoint": "recipe",
                    "request_body": {
                        "recipe_id": recipeId
                    }
                }), "ExternalServices"
            );

            // Check if the response returned something 
            // and check if the response has the proper format 
            if (apiResponse !== undefined && apiResponse.hasOwnProperty('errors') && apiResponse.hasOwnProperty('document')) {

                let errors: string[] = Object.getOwnPropertyDescriptor(apiResponse, 'errors')?.value || [];
                if (errors.length === 0) {

                    // Get the documents
                    let document: Object = Object.getOwnPropertyDescriptor(apiResponse, 'document')?.value || "";

                    console.log(document);

                    if (document !== undefined && document !== "") {
                        return RecipeModel.jsonToSequentialPageModel(document);
                    }

                }

            }
        }


        return undefined;

    }


}