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

import { SequentialPageCheckboxModel } from "./SequentialPageCheckboxModel"
import { ISequentialPageModel } from "./SequentialPageModel"

export class RecipeModel implements ISequentialPageModel {

    readonly id: string
    readonly hashedId: string
    readonly pageId: string
    readonly pageTitle: string
    readonly durationMinutesCooking: number
    readonly durationMinutesPrep: number
    readonly durationMinutesTotal: number
    readonly requiredEquipment: string[]
    readonly requiredIngredients: string[]
    readonly sentences: string[]
    readonly stepsSentences: string[][]
    readonly stepsImages: string[][]
    stepsSentencesWithImages: string[][]

    constructor(model: ISequentialPageModel,
        durationMinutesCooking: number,
        durationMinutesPrep: number,
        durationMinutesTotal: number,
        requiredEquipment: string[],
        requiredIngredients: string[]) {
        this.id = model.id;
        this.hashedId = model.hashedId;
        this.pageId = model.pageId;
        this.pageTitle = model.pageTitle;
        this.durationMinutesCooking = durationMinutesCooking
        this.durationMinutesPrep = durationMinutesPrep
        this.durationMinutesTotal = durationMinutesTotal
        this.requiredEquipment = requiredEquipment
        this.requiredIngredients = requiredIngredients
        this.sentences = model.sentences
        this.stepsSentences = model.stepsSentences
        this.stepsImages = model.stepsImages
        this.stepsSentencesWithImages = model.stepsSentencesWithImages
    }


    static jsonToSequentialPageModel = (jsonObject: object): ISequentialPageModel => {
        let jsonObj = JSON.parse(JSON.stringify(jsonObject))

        let recipeModelObj = new RecipeModel({
            id: jsonObj['id'],
            hashedId: jsonObj['hashed_id'],
            pageId: jsonObj['page_id'],
            pageTitle: jsonObj['page_title'],
            sentences: jsonObj['sentences'],
            stepsSentences: jsonObj['steps_sentences'],
            stepsImages: jsonObj['steps_images'],
            stepsSentencesWithImages: [],
        },
            jsonObj['duration_minutes_cooking'],
            jsonObj['duration_minutes_prep'],
            jsonObj['duration_minutes_total'],
            jsonObj['required_equipment'],
            jsonObj['required_ingredient']);

        // Generate the field with sentences and images
        if (recipeModelObj.stepsSentences !== undefined && recipeModelObj.stepsImages !== undefined) {
            let tempStepsSentencesWithImages = recipeModelObj.stepsSentences.map(function (el, index) {
                return el.concat(recipeModelObj.stepsImages[index]);
            });
            recipeModelObj.stepsSentencesWithImages = tempStepsSentencesWithImages;

        }

        return recipeModelObj;
    }

    sequentialPageModelToSequentialPageCheckboxModel = (section: string, sectionValue: string): SequentialPageCheckboxModel => {
        return new SequentialPageCheckboxModel({
            id: this.id,
            hashedId: this.hashedId,
            pageId: this.pageId,
            pageTitle: this.pageTitle,
            section: section,
            sectionValue: sectionValue

        });
    }
}
