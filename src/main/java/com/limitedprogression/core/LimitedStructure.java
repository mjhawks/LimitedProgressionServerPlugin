package com.limitedprogression.core;

import org.bukkit.generator.structure.GeneratedStructure;

public class LimitedStructure {
    private GeneratedStructure genStruct;
    public LimitedStructure(GeneratedStructure newGenStruct){
        genStruct = newGenStruct;
    }
    public GeneratedStructure getGenStruct(){
        return genStruct;
    }
    @Override
    public boolean equals(Object obj){
        if(obj == null){
            return  false;
        }
        if(obj.getClass() != LimitedStructure.class){
            return false;
        }
        else{
            GeneratedStructure objStruct = ((LimitedStructure) obj).getGenStruct();
            if(genStruct.getBoundingBox() == objStruct.getBoundingBox()){// && genStruct.getStructure().getStructureType() == objStruct.getStructure().getStructureType()){
                return true;
            }
            else{
                return true;
            }
        }
    }
}

