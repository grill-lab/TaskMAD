/*
 * Copyright 2018. University of Southern California
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { RecipeCheckboxModel } from "./RecipeCheckboxModel";

export interface IRecipeModel {
    readonly id: string
    readonly hashedId: string
    readonly pageId: string
    readonly pageTitle: string
    readonly durationMinutesCooking: number
    readonly durationMinutesPrep: number
    readonly durationMinutesTotal: number
    readonly requiredEquipment: string[]
    readonly requiredIngredient: string[]
    readonly sentences: string[]
    readonly stepsSentences: string[][]
    readonly stepsImages: string[][]
    stepsSentencesWithImages: string[][]
}

export class RecipeModel implements IRecipeModel {
    constructor(model: IRecipeModel) {
        this.id = model.id;
        this.hashedId = model.hashedId;
        this.pageId = model.pageId;
        this.pageTitle = model.pageTitle;
        this.durationMinutesCooking = model.durationMinutesCooking
        this.durationMinutesPrep = model.durationMinutesPrep
        this.durationMinutesTotal = model.durationMinutesTotal
        this.requiredEquipment = model.requiredEquipment
        this.requiredIngredient = model.requiredIngredient
        this.sentences = model.sentences
        this.stepsSentences = model.stepsSentences
        this.stepsImages = model.stepsImages
        this.stepsSentencesWithImages = model.stepsSentencesWithImages
    }

    readonly id: string
    readonly hashedId: string
    readonly pageId: string
    readonly pageTitle: string
    readonly durationMinutesCooking: number
    readonly durationMinutesPrep: number
    readonly durationMinutesTotal: number
    readonly requiredEquipment: string[]
    readonly requiredIngredient: string[]
    readonly sentences: string[]
    readonly stepsSentences: string[][]
    readonly stepsImages: string[][]
    stepsSentencesWithImages: string[][]

    public static jsonToRecipeModel = (jsonRecipe: object): RecipeModel => {
        let jsonObj = JSON.parse(JSON.stringify(jsonRecipe))
        
        let recipeModelObj = new RecipeModel({
            id: jsonObj['id'],
            hashedId: jsonObj['hashed_id'],
            pageId: jsonObj['page_id'],
            pageTitle: jsonObj['page_title'],
            durationMinutesCooking: jsonObj['duration_minutes_cooking'],
            durationMinutesPrep: jsonObj['duration_minutes_prep'],
            durationMinutesTotal: jsonObj['duration_minutes_total'],
            requiredEquipment: jsonObj['required_equipment'],
            requiredIngredient: jsonObj['required_ingredient'],
            sentences: jsonObj['sentences'],
            stepsSentences: jsonObj['steps_sentences'],
            stepsImages: jsonObj['steps_images'],
            stepsSentencesWithImages: []
        });

        // Generate the field with sentences and images
        if(recipeModelObj.stepsSentences !== undefined && recipeModelObj.stepsImages !== undefined){
            var tempStepsSentencesWithImages = recipeModelObj.stepsSentences.map(function(el, index) {
                var mergedArrays = el.concat(recipeModelObj.stepsImages[index]);
                return mergedArrays;
              });
              recipeModelObj.stepsSentencesWithImages = tempStepsSentencesWithImages;
              
        }

        return recipeModelObj;
    }

    public recipeModelToRecipeCheckboxModel = (section: string, sectionValue: string): RecipeCheckboxModel =>{
        let tempRecipeCheckboxModel = new RecipeCheckboxModel({
            id: this.id,
            hashedId: this.hashedId,
            pageId: this.pageId,
            pageTitle: this.pageTitle,
            section: section,
            sectionValue: sectionValue

        });
        return tempRecipeCheckboxModel;
    }
}
