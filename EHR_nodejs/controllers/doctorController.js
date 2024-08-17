
const asyncHandler = require('express-async-handler')

const Doctor = require('../models/doctorModel.js')
const getDoctors = asyncHandler(async (req,res) => {
    const doctors = await Doctor.find()
    res.json(doctors)
});

const addDoctor =asyncHandler(async (req,res)=>{
    const {name,email,password}=req.body;
    if(!name || !email || !password )
    {
       throw new Error("All fields are mandatory")
    }
    const doctor= await Doctor.create({
        name,email,password
    })
    res.status(200).json(doctor)
});

const getEhrs = asyncHandler(async (req,res)=>{
    console.log('Ehr list') 
});

module.exports = {getDoctors,addDoctor,getEhrs}
