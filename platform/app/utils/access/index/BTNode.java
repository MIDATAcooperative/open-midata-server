package utils.access.index;

import utils.exceptions.InternalServerException;

public class BTNode
{
    public final static int MIN_DEGREE          =   100;
    public final static int LOWER_BOUND_KEYNUM  =   MIN_DEGREE - 1;
    public final static int UPPER_BOUND_KEYNUM  =   (MIN_DEGREE * 2) - 1;
   


    protected static IndexPage getChildNodeAtIndex(IndexPage btNode, int keyIdx, int nDirection) throws InternalServerException  {
        if (btNode.mIsLeaf) {
            return null;
        }

        keyIdx += nDirection;
        if ((keyIdx < 0) || (keyIdx > btNode.mCurrentKeyNum)) {
            return null;
        }

        return btNode.getChild(keyIdx);
    }


    protected static IndexPage getLeftChildAtIndex(IndexPage btNode, int keyIdx) throws InternalServerException  {
        return getChildNodeAtIndex(btNode, keyIdx, 0);
    }


    protected static IndexPage getRightChildAtIndex(IndexPage btNode, int keyIdx) throws InternalServerException  {
        return getChildNodeAtIndex(btNode, keyIdx, 1);
    }


    protected static IndexPage getLeftSiblingAtIndex(IndexPage parentNode, int keyIdx) throws InternalServerException  {
        return getChildNodeAtIndex(parentNode, keyIdx, -1);
    }


    protected static IndexPage getRightSiblingAtIndex(IndexPage parentNode, int keyIdx) throws InternalServerException  {
        return getChildNodeAtIndex(parentNode, keyIdx, 1);
    }
}