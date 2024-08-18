const { Wallets } = require('fabric-network');
const FabricCAServices = require('fabric-ca-client');
const path = require('path');
const fs = require('fs');

function capitalizeFirstLetter(string) {
    return string.charAt(0).toUpperCase() + string.slice(1);
}

async function getCCP(org){
    try{
    const ccpPath = path.resolve(__dirname,'..','connection-profiles',org,`connection-${org}.json`);
    const ccpJSON = fs.readFileSync(ccpPath,'utf8');
    const ccp = JSON.parse(ccpJSON);
    return ccp;
    }
    catch(error){
           console.error(error);
           throw new Error(error);
    }
}

async function getCaUrl(org,ccp) {
    try{
        const caUrl = ccp.certificateAuthorities[`ca.${org}.example.com`].url;
        return caUrl;
    }catch(err){
        console.error(err);
        throw new Error(err);
    }
    
}

async function enrollAdmin(userOrg, walletPath) {
    try {
        const ccp = await getCCP(userOrg);
        const caUrl = await getCaUrl(userOrg,ccp);
        const ca = new FabricCAServices(caUrl);
        const wallet = await Wallets.newFileSystemWallet(walletPath);
        console.log(walletPath);
        const identity = await wallet.get('admin');
        if (identity) {
            console.log('An identity for the admin already exists in the wallet');
            return;
        }

        const enrollment = await ca.enroll({ enrollmentID: 'admin', enrollmentSecret: 'adminpw' });
        const x509Identity = {
            credentials: {
                certificate: enrollment.certificate,
                privateKey: enrollment.key.toBytes()
            },
            mspId: `${capitalizeFirstLetter(userOrg)}MSP`,
            type: 'X.509'
        };
        await wallet.put('admin', x509Identity);
        console.log('Successfully enrolled admin user and imported it into the wallet');
    } catch (error) {
        console.error(error);
    }
}

async function registerUser(username, userOrg) {
    try {
        const ccp = await getCCP(userOrg);
        const caUrl = await getCaUrl(userOrg,ccp);
        const ca = new FabricCAServices(caUrl);
        const walletPath = path.join(__dirname, '..', 'connection-profiles', userOrg, 'wallet');
        const wallet = await Wallets.newFileSystemWallet(walletPath);

        const userExists = await wallet.get(username);
        if (userExists) {
            console.log(`An identity for the user ${username} already exists in the wallet`);
            return;
        }

        const adminExists = await wallet.get('admin');
        if (!adminExists) {
            await enrollAdmin(userOrg,walletPath);
        }
        const adminIdentity = await wallet.get('admin');
        const provider = wallet.getProviderRegistry().getProvider(adminIdentity.type);
        const adminUser = await provider.getUserContext(adminIdentity, 'admin');

        const secret = await ca.register({
            affiliation: `${userOrg}.department1`,
            enrollmentID: username,
            role: 'client'
        }, adminUser);

        const enrollment = await ca.enroll({
            enrollmentID: username,
            enrollmentSecret: secret
        });

        const x509Identity = {
            credentials: {
                certificate: enrollment.certificate,
                privateKey: enrollment.key.toBytes()
            },
            mspId: `${capitalizeFirstLetter(userOrg)}MSP`,
            type: 'X.509'
        };

        await wallet.put(username, x509Identity);
        console.log(`Successfully registered and enrolled user ${username} and imported it into the wallet`);
    } catch (err) {
        console.error(err);
    }
}

async function removeUser(username, userOrg) {
    try {
        const ccp = await getCCP(userOrg);
        const caUrl = await getCaUrl(userOrg, ccp);
        const ca = new FabricCAServices(caUrl);
        const walletPath = path.join(__dirname, '..', 'connection-profiles', userOrg, 'wallet');
        const wallet = await Wallets.newFileSystemWallet(walletPath);

        // Check if user exists in the wallet
        const userExists = await wallet.get(username);
        if (!userExists) {
            console.log(`No wallet found for ${username}`);
            
        } else {
            // Remove user from wallet
            await wallet.remove(username);
            console.log(`User ${username} removed from wallet.`);
        }

        // Ensure admin exists in the wallet
        const adminExists = await wallet.get('admin');
        if (!adminExists) {
            console.log('An identity for the admin user does not exist in the wallet');
            await enrollAdmin(userOrg, walletPath); // Enroll admin if not exists
        }

        // Revoke user from CA
        const adminIdentity = await wallet.get('admin');
        const provider = wallet.getProviderRegistry().getProvider(adminIdentity.type);
        const adminUser = await provider.getUserContext(adminIdentity, 'admin');

        await ca.revoke({ enrollmentID: username }, adminUser);
        console.log(`Successfully revoked user ${username} from the CA and removed from wallet.`);
    } catch (err) {
        console.error(`Failed to remove user ${username}: ${err}`);
        throw new Error(err);
    }
}

registerUser('user1', 'org1');