const express= require('express')
const router = express.Router();
const {getDoctors,addDoctor,getEhrs}= require('../controllers/doctorController')
router.route('/').get(getDoctors).post(addDoctor)

module.exports = router;