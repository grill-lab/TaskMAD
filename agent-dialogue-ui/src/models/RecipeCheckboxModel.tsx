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


// Class used in order to keep track of the different checkboxes associated 
// to the different parts of a recipe
export interface IRecipeCheckboxModel {
    readonly id: string
    readonly hashedId: string
    readonly pageId: string
    readonly pageTitle: string
    readonly section: string
    readonly sectionValue: string
    clickedTimestamp?: Date
}

export class RecipeCheckboxModel implements IRecipeCheckboxModel {
    constructor(model: IRecipeCheckboxModel) {
        this.id = model.id;
        this.hashedId = model.hashedId;
        this.pageId = model.pageId;
        this.pageTitle = model.pageTitle;
        this.section = model.section;
        this.sectionValue = model.sectionValue;
    }

    readonly id: string
    readonly hashedId: string
    readonly pageId: string
    readonly pageTitle: string
    readonly section: string
    readonly sectionValue: string
    clickedTimestamp?: Date

    public isEqual = (model: RecipeCheckboxModel):boolean => {
        return this.id === model.id 
        && this.hashedId === model.hashedId
        && this.pageId === model.pageId
        && this.pageTitle === model.pageTitle
        && this.section === model.section
        && this.sectionValue === model.sectionValue
    }
}

