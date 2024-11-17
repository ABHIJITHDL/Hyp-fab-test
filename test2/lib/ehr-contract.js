const { Contract } = require('fabric-contract-api');

class EhrContract extends Contract {

    async createEHRRecord(ctx, doctorId, patientId, hash,timestamp) {
        const ehrRecord = {
            patientId,
            doctorId,
            hash,
            timestamp,
            transactions: [],  // Array to store transaction history
        };

        const compositeKey = ctx.stub.createCompositeKey('EHR', [patientId, doctorId]);
        await ctx.stub.putState(compositeKey, Buffer.from(JSON.stringify(ehrRecord)));
        
        // Record the EHR creation action
        ehrRecord.transactions.push({
            type: 'creation',
            timestamp,
            hash
        });

        await ctx.stub.putState(compositeKey, Buffer.from(JSON.stringify(ehrRecord)));
        return JSON.stringify(ehrRecord);
    }

    async updateEHRRecord(ctx, doctorId, patientId, newHash,timestamp) {
        const compositeKey = ctx.stub.createCompositeKey('EHR', [patientId, doctorId]);
        const recordJSON = await ctx.stub.getState(compositeKey);
        
        if (!recordJSON || recordJSON.length === 0) {
            throw new Error(`EHR record for patient ${patientId} and doctor ${doctorId} does not exist`);
        }
        
        const ehrRecord = JSON.parse(recordJSON.toString());

        // Ensure that only the doctor who created the EHR can update it

        ehrRecord.hash = newHash;
        ehrRecord.timestamp = timestamp

        // Add an update transaction entry
        ehrRecord.transactions.push({
            type: 'update',
            timestamp,
            hash: newHash,
        });

        await ctx.stub.putState(compositeKey, Buffer.from(JSON.stringify(ehrRecord)));
        return JSON.stringify(ehrRecord);
    }

    async recordAccess(ctx, doctorId, patientId,hash,timestamp) {
        const compositeKey = ctx.stub.createCompositeKey('EHR', [patientId,doctorId]);
        const recordJSON = await ctx.stub.getState(compositeKey);
        
        if (!recordJSON || recordJSON.length === 0) {
            throw new Error(`EHR record for patient ${patientId} and doctor ${doctorId} does not exist`);
        }
        
        const ehrRecord = JSON.parse(recordJSON.toString());

        // Add an access transaction entry
        ehrRecord.transactions.push({
            type: 'access',
            timestamp,
            hash
        });

        await ctx.stub.putState(compositeKey, Buffer.from(JSON.stringify(ehrRecord)));
        return JSON.stringify(ehrRecord);
    }

    async getEHRRecord(ctx, patientId, doctorId) {
        const compositeKey = ctx.stub.createCompositeKey('EHR', [patientId, doctorId]);
        const recordJSON = await ctx.stub.getState(compositeKey);
        
        if (!recordJSON || recordJSON.length === 0) {
            throw new Error(`EHR record for patient ${patientId} and doctor ${doctorId} does not exist`);
        }
        
        return recordJSON.toString();
    }

    async getAllEHRRecordByPatient(ctx,patientId){
        const iterator = await ctx.getStateByPartialCompositeKey('EHR',patientId);
        const results = [];
        while (true){
            const res = await iterator.next();
            if(res.value){
                results.push(JSON.parse(res.value.value.toString()));
            }
            if(res.done){
                await iterator.close();
                break;
            }
        }
        return JSON.stringify(results);
    }

    async getAllEHRRecordByDoctor(ctx,doctorId){
        const iterator = await ctx.getStateByPartialCompositeKey('EHR',doctorId);
        const results = [];
        while(true){
            const res = await iterator.next();
            if(res.value){
                results.push(JSON.parse(res.value.value.toString()));
            }
            if(res.done){
                await iterator.close();
                break;
            }
        }
        return JSON.stringify(results);
    }

    async getAccessHistory(ctx, patientId, doctorId) {
        const compositeKey = ctx.stub.createCompositeKey('EHR', [patientId, doctorId]);
        const recordJSON = await ctx.stub.getState(compositeKey);
        
        if (!recordJSON || recordJSON.length === 0) {
            throw new Error(`EHR record for patient ${patientId} and doctor ${doctorId} does not exist`);
        }
        
        const ehrRecord = JSON.parse(recordJSON.toString());
        const accessHistory = ehrRecord.transactions.filter(transaction => transaction.type === 'access' && transaction.doctorId === doctorId);
        
        return JSON.stringify(accessHistory);
    }

    async getAllEHRRecordsForPatient(ctx, patientId) {
        const iterator = await ctx.stub.getStateByPartialCompositeKey('EHR', [patientId]);
        const results = [];
        
        while (true) {
            const res = await iterator.next();
            if (res.value) {
                results.push(JSON.parse(res.value.value.toString()));
            }
            if (res.done) {
                await iterator.close();
                break;
            }
        }

        return JSON.stringify(results);
    }
}

module.exports = EhrContract;
