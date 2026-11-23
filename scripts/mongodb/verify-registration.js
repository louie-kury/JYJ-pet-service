// 실행: mongosh '<URI>/<대상 DB>' --file scripts/mongodb/verify-registration.js
(() => {
    const index = db.pets.getIndexes().find(item => item.name === "registration_request_key_unique");
    if (!index || index.unique !== true || Object.keys(index.key).length !== 1
            || index.key["registration.requestKey"] !== 1
            || index.partialFilterExpression?.["registration.requestKey"]?.$type !== "string"
            || index.sparse === true) {
        throw new Error("등록 요청 키의 partial unique 인덱스가 없거나 정의가 다릅니다.");
    }
    for (const code of ["DOG", "CAT"]) {
        const matches = db.pet_species.find({code, active: true}).toArray();
        if (matches.length !== 1 || typeof matches[0]._id !== "string") {
            throw new Error("활성 " + code + " 종 기준 데이터를 확인하세요.");
        }
        print(code + " speciesId=" + matches[0]._id);
    }
    print("등록 준비 확인 완료: " + db.getName());
})();
