const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors'); // Import the cors package
const { invokeTransaction } = require('./fabric/invoke');

const app = express();
const port = 3000;

// Enable CORS for all routes and origins
app.use(cors());

app.use(bodyParser.json());

app.post('/invoke', async (req, res) => {
    const { channelName, chaincodeName, fcn, args, username, userOrg } = req.body;
    try {
        const result = await invokeTransaction(channelName, chaincodeName, fcn, args, username, userOrg);
        res.status(200).json(result);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.listen(port, () => {
    console.log(`Server is running on port ${port}`);
});