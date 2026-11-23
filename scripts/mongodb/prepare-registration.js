// 실행: mongosh '<URI>/<대상 DB>' --file scripts/mongodb/prepare-registration.js
// 대상 DB를 명시한 뒤 실행한다. 기존 종·품종은 덮어쓰지 않는다.
(() => {
    if (["admin", "config", "local", "test"].includes(db.getName())) {
        throw new Error("반려동물 서비스의 대상 DB를 명시하세요.");
    }
    const seeds = [
        {_id: "dog", code: "DOG", name: "강아지", active: true, breeds: []},
        {_id: "cat", code: "CAT", name: "고양이", active: true, breeds: []}
    ];
    const existingSpecies = db.pet_species.find().toArray();
    const codes = new Set();
    for (const species of existingSpecies) {
        if (typeof species._id !== "string" || typeof species.code !== "string"
                || typeof species.name !== "string" || typeof species.active !== "boolean"
                || !Array.isArray(species.breeds) || codes.has(species.code)) {
            throw new Error("기존 종 기준 데이터의 타입·중복을 확인하세요.");
        }
        codes.add(species.code);
        const ids = new Set();
        for (const breed of species.breeds) {
            if (typeof breed.breedId !== "string" || !breed.breedId.trim()
                    || typeof breed.name !== "string" || typeof breed.active !== "boolean"
                    || ids.has(breed.breedId)) {
                throw new Error("기존 품종 기준 데이터의 타입·중복을 확인하세요.");
            }
            ids.add(breed.breedId);
        }
    }
    for (const seed of seeds) {
        const existingId = db.pet_species.findOne({_id: seed._id});
        const existingCode = db.pet_species.findOne({code: seed.code});
        if ((existingId && existingId.code !== seed.code)
                || (existingCode && existingCode._id !== seed._id)) {
            throw new Error("기본 종 ID/code가 기존 데이터와 다릅니다. 적용 전에 매핑을 검토하세요.");
        }
    }
    db.pets.createIndex(
        {"registration.requestKey": 1},
        {name: "registration_request_key_unique", unique: true,
            partialFilterExpression: {"registration.requestKey": {$type: "string"}}}
    );
    db.pet_species.createIndex({code: 1}, {name: "species_code_unique", unique: true});
    for (const seed of seeds) {
        db.pet_species.updateOne({_id: seed._id}, {$setOnInsert: seed}, {upsert: true});
    }
    print("등록 인덱스 및 기본 종 데이터 준비 완료. verify-registration.js로 확인하세요.");
})();
