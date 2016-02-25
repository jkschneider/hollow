package com.netflix.vms.transformer.hollowoutput;

import java.util.Arrays;

public class ArtWorkImageRecipe implements Cloneable {

    public char[] recipeNameStr = null;
    public char[] extensionStr = null;
    public char[] cdnFolderStr = null;
    public char[] hostNameStr = null;

    public boolean equals(Object other) {
        if(other == this)  return true;
        if(!(other instanceof ArtWorkImageRecipe))
            return false;

        ArtWorkImageRecipe o = (ArtWorkImageRecipe) other;
        if(!Arrays.equals(o.recipeNameStr, recipeNameStr)) return false;
        if(!Arrays.equals(o.extensionStr, extensionStr)) return false;
        if(!Arrays.equals(o.cdnFolderStr, cdnFolderStr)) return false;
        if(!Arrays.equals(o.hostNameStr, hostNameStr)) return false;
        return true;
    }

    public ArtWorkImageRecipe clone() {
        try {
            ArtWorkImageRecipe clone = (ArtWorkImageRecipe)super.clone();
            clone.__assigned_ordinal = -1;
            return clone;
        } catch (CloneNotSupportedException cnse) { throw new RuntimeException(cnse); }
    }

    @SuppressWarnings("unused")
    private int __assigned_ordinal = -1;
}