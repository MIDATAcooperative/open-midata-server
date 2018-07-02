class observation {
    constructor(data) {
        this.resourceType = "Observation";
        this.status = "preliminary"; //registered | preliminary | final | amended
        this.category = [
            {
                coding: [
                    {
                        system: "http://hl7.org/fhir/observation-category",
                        code: "vital-signs",
                        display: "Vital Signs"
                    }
                ],
                text: "Vital Signs"
            }
        ];
        this.code = {
            coding: [
                {
                    system: "http://loinc.org",
                    code: "8310-5",
                    display: "Body temperature"
                }
            ],
            text: "Body temperature"
        };

        this.effectiveDateTime = new Date().toISOString();

        this.valueQuantity = {
            //value: 36.5,
            unit: "°C",
            system: "http://unitsofmeasure.org",
            code: "Cel"
        };

        if (data && data.valueQuantity) {
            this.valueQuantity.value = data.valueQuantity;
        } else {
            // set a random value
            this.valueQuantity.value = Math.floor(Math.random() * 5) + 36 + (Math.floor(Math.random() * 100) / 100);
        }
        
        if (data) {
            if (data.id) {
                this.id = data.id;
            }

            if (data.versionId) {
                this.versionId = data.versionId;
            }
        }
    }

    set v_value(value){
        this.valueQuantity.value = value;
    }

    get v_value(){
        return this.valueQuantity.value;
    }

    get c_system(){
        return this.code.coding[0].system;
    }

    get c_code(){
        return this.code.coding[0].code;
    }
}

module.exports = observation;