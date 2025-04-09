const { Contract } = require('fabric-contract-api');

class EhrContract extends Contract {

    async initLedger(ctx) {
        return 'Initialized the ledger';
    }

    async createEHRRecord(ctx, doctorId, patientId, hash, timestamp) {
        try {
            const ehrRecord = {
                patientId,
                doctorId,
                hash,
                status: 'active',
                timestamp,
                transactions: [{
                    type: 'creation',
                    timestamp,
                    hash
                }]
            };

            // Primary record
            const primaryKey = ctx.stub.createCompositeKey( 'EHR', [patientId, doctorId]);
            await ctx.stub.putState(primaryKey, Buffer.from(JSON.stringify(ehrRecord)));

            // Secondary index for doctor
            const doctorIndex = ctx.stub.createCompositeKey('DOCTOR_INDEX', [doctorId, patientId]);
            await ctx.stub.putState(doctorIndex, Buffer.from(primaryKey));

            return JSON.stringify(ehrRecord);
        } catch (error) {
            throw new Error(`Failed to create EHR record: ${error.message}`);
        }
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

    async revokeAccess(ctx, doctorId, patientId, hash, timestamp) {
        const compositeKey = ctx.stub.createCompositeKey('EHR', [patientId, doctorId]);
        const recordJSON = await ctx.stub.getState(compositeKey);
        
        if (!recordJSON || recordJSON.length === 0) {
            throw new Error(`EHR record for patient ${patientId} and doctor ${doctorId} does not exist`);
        }
        
        const ehrRecord = JSON.parse(recordJSON.toString());
        ehrRecord.status = 'revoked';

        // Add a revoke transaction entry
        ehrRecord.transactions.push({
            type: 'revoke',
            timestamp,
            hash
        });

        await ctx.stub.putState(compositeKey, Buffer.from(JSON.stringify(ehrRecord)));
        return JSON.stringify(ehrRecord);
    }

    async activateAccess(ctx, doctorId, patientId, hash, timestamp) {
        const compositeKey = ctx.stub.createCompositeKey('EHR', [patientId, doctorId]);
        const recordJSON = await ctx.stub.getState(compositeKey);

        if (!recordJSON || recordJSON.length === 0) {
            throw new Error(`EHR record for patient ${patientId} and doctor ${doctorId} does not exist`);
        }

        const ehrRecord = JSON.parse(recordJSON.toString());
        ehrRecord.status = 'active';
        ehrRecord.transactions.push({
            type: 'activate',
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
        const iterator = await ctx.stub.getStateByPartialCompositeKey('EHR',[patientId]);
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

    async getAllEHRRecordByDoctor(ctx, doctorId) {
        try {
            const iterator = await ctx.stub.getStateByPartialCompositeKey('DOCTOR_INDEX', [doctorId]);
            const results = [];

            while (true) {
                const res = await iterator.next();
                if (res.value) {
                    // Get primary key from index
                    const primaryKey = res.value.value.toString();
                    // Fetch actual record
                    const ehrRecord = await ctx.stub.getState(primaryKey);
                    if (ehrRecord) {
                        results.push(JSON.parse(ehrRecord.toString()));
                    }
                }
                if (res.done) {
                    await iterator.close();
                    break;
                }
            }
            return JSON.stringify(results);
        } catch (error) {
            throw new Error(`Failed to get EHR records by doctor: ${error.message}`);
        }
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
}

module.exports = EhrContract;
