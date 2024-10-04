const { Contract } = require('fabric-contract-api');

class EhrContract extends Contract {

    async createEHRRecord(ctx, ehrId, doctorId, patientId, hash,timestamp) {
        const ehrRecord = {
            ehrId,
            doctorId,
            patientId,
            hash,
            timestamp,
            transactions: [],  // Array to store transaction history
            accessLog: {}  // Object to log access details
        };

        const compositeKey = ctx.stub.createCompositeKey('EHR', [patientId, ehrId]);
        await ctx.stub.putState(compositeKey, Buffer.from(JSON.stringify(ehrRecord)));
        
        // Record the EHR creation action
        ehrRecord.transactions.push({
            type: 'creation',
            doctorId,
            patientId,
            timestamp,
            details: `EHR created by doctor ${doctorId}`
        });

        await ctx.stub.putState(compositeKey, Buffer.from(JSON.stringify(ehrRecord)));
        return JSON.stringify(ehrRecord);
    }

    async updateEHRRecord(ctx, ehrId, doctorId, patientId, newHash) {
        const compositeKey = ctx.stub.createCompositeKey('EHR', [patientId, ehrId]);
        const recordJSON = await ctx.stub.getState(compositeKey);
        
        if (!recordJSON || recordJSON.length === 0) {
            throw new Error(`EHR record for patient ${patientId} with ID ${ehrId} does not exist`);
        }
        
        const ehrRecord = JSON.parse(recordJSON.toString());

        // Ensure that only the doctor who created the EHR can update it

        ehrRecord.hash = newHash;
        ehrRecord.timestamp = Date.now().toString();

        // Add an update transaction entry
        ehrRecord.transactions.push({
            type: 'update',
            doctorId,
            patientId,
            timestamp: ehrRecord.timestamp,
            details: `EHR updated with new hash ${newHash}`
        });

        await ctx.stub.putState(compositeKey, Buffer.from(JSON.stringify(ehrRecord)));
        return JSON.stringify(ehrRecord);
    }

    async recordAccess(ctx, ehrId, doctorId, patientId) {
        const compositeKey = ctx.stub.createCompositeKey('EHR', [patientId, ehrId]);
        const recordJSON = await ctx.stub.getState(compositeKey);
        
        if (!recordJSON || recordJSON.length === 0) {
            throw new Error(`EHR record for patient ${patientId} with ID ${ehrId} does not exist`);
        }
        
        const ehrRecord = JSON.parse(recordJSON.toString());

        // Add an access transaction entry
        ehrRecord.transactions.push({
            type: 'access',
            doctorId,
            patientId,
            timestamp: Date.now().toString(),
            details: `Doctor ${doctorId} accessed EHR`
        });

        await ctx.stub.putState(compositeKey, Buffer.from(JSON.stringify(ehrRecord)));
        return JSON.stringify(ehrRecord);
    }

    async getEHRRecord(ctx, ehrId, patientId) {
        const compositeKey = ctx.stub.createCompositeKey('EHR', [patientId, ehrId]);
        const recordJSON = await ctx.stub.getState(compositeKey);
        
        if (!recordJSON || recordJSON.length === 0) {
            throw new Error(`EHR record for patient ${patientId} with ID ${ehrId} does not exist`);
        }
        
        return recordJSON.toString();
    }

    async getAccessHistory(ctx, ehrId, patientId, doctorId) {
        const compositeKey = ctx.stub.createCompositeKey('EHR', [patientId, ehrId]);
        const recordJSON = await ctx.stub.getState(compositeKey);
        
        if (!recordJSON || recordJSON.length === 0) {
            throw new Error(`EHR record for patient ${patientId} with ID ${ehrId} does not exist`);
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
