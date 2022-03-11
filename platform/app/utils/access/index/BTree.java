/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.access.index;

import java.util.Stack;

import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

public class BTree<A extends BaseIndexKey<A,B>,B>
  {
      public final static int     REBALANCE_FOR_LEAF_NODE         =   1;
      public final static int     REBALANCE_FOR_INTERNAL_NODE     =   2;
  
      private IndexPage<A,B> mRoot = null;
      private BaseIndexRoot<A,B> root;
      private long  mSize = 0L;
      private IndexPage<A,B> mIntermediateInternalNode = null;
      private int mNodeIdx = 0;
      private final Stack<StackInfo> mStackTracer = new Stack<StackInfo>();
    
  
      public BTree(BaseIndexRoot<A,B> root, IndexPage<A,B> mroot) {
    	  this.mRoot = mroot;
    	  this.root = root;
      }
  
      //
      // Get the root node
      //
      public IndexPage<A,B> getRootNode() {
          return mRoot;
      }
  
  
      //
      // The total number of nodes in the tree
      //
      public long size() {
          return mSize;
      }
  
  
     //
      // Clear all the entries in the tree
      //
      public void clear() {
          mSize = 0L;
          mRoot = null;
      }
  
  
      //
      // Create a node with default values
      //
      private IndexPage<A,B> createNode() throws InternalServerException {
          IndexPage<A,B> btNode;
          btNode = new IndexPage<A,B>(mRoot.key, root);
          btNode.mIsLeaf = true;
          btNode.mCurrentKeyNum = 0;
          root.loadedPages.put(btNode.getId(), btNode);
          return btNode;
      }
  
  
     //
      // Search value for a specified key of the tree
      //
      public A search(A key) throws InternalServerException, LostUpdateException {
    	  if (mRoot.mCurrentKeyNum == 0) return null;
    	  
          IndexPage<A,B> currentNode = mRoot;
          A currentKey;
          int i, numberOfKeys;
          
          while (currentNode != null) {
              numberOfKeys = currentNode.mCurrentKeyNum;
              i = 0;
              currentKey = (A) currentNode.mKeys[i];
              while ((i < numberOfKeys) && (key.compareTo(currentKey) > 0)) {
                  ++i;
                  if (i < numberOfKeys) {
                      currentKey = (A) currentNode.mKeys[i];
                  }
                  else {
                      --i;
                      break;
                  }
              }
  
              if ((i < numberOfKeys) && (key.compareTo(currentKey) == 0)) {
                  return currentKey;
              }
  
              // We don't need it
              /*
              if (currentNode.mIsLeaf) {
                  return null;
              }
              */
  
              if (key.compareTo(currentKey) > 0) {
                  currentNode = BaseIndexRoot.getRightChildAtIndex(currentNode, i);
              }
              else {
                  currentNode = BaseIndexRoot.getLeftChildAtIndex(currentNode, i);
              }
          }
  
          return null;
      }
              
  
      //
      // Insert key and its value into the tree
      //
      public BTree insert(A key) throws InternalServerException, LostUpdateException {
          if (mRoot == null) {
              mRoot = createNode();
          }
  
          ++mSize;
          if (mRoot.mCurrentKeyNum == root.UPPER_BOUND_KEYNUM()) {
              // The root is full, split it
              IndexPage<A,B> btNode = createNode();
              btNode.copyFrom(mRoot);                            
              mRoot.initNonLeaf();                            
              mRoot.mChildren[0] = btNode.getId();
              
              splitNode(mRoot, 0, btNode);
          }
  
          insertKeyAtNode(mRoot, key);
          return this;
      }
  
  
      //
      // Insert key and its value to the specified root
      //
      private void insertKeyAtNode(IndexPage<A,B> rootNode, A key) throws InternalServerException, LostUpdateException {
          int i;
          int currentKeyNum = rootNode.mCurrentKeyNum;
  
          if (rootNode.mIsLeaf) {
              if (rootNode.mCurrentKeyNum == 0) {
                  // Empty root
                  rootNode.mKeys[0] = key;
                  ++(rootNode.mCurrentKeyNum);
                  rootNode.changed = true;
                  return;
              }
  
              // Verify if the specified key doesn't exist in the node
              for (i = 0; i < rootNode.mCurrentKeyNum; ++i) {
                  if (key.compareTo(rootNode.mKeys[i]) == 0) {
                      // Find existing key, overwrite its value only
                      rootNode.mKeys[i] = key;
                      rootNode.changed = true;
                      --mSize;
                      return;
                  }
              }
  
              i = currentKeyNum - 1;
              A existingKeyVal = rootNode.mKeys[i];
              while ((i > -1) && (key.compareTo(existingKeyVal) < 0)) {
                  rootNode.mKeys[i + 1] = existingKeyVal;
                  --i;
                  if (i > -1) {
                      existingKeyVal = rootNode.mKeys[i];
                  }
              }
  
              i = i + 1;
              rootNode.mKeys[i] = key.copy();
              rootNode.changed = true;
              ++(rootNode.mCurrentKeyNum);
              return;
          }
  
          // This is an internal node (i.e: not a leaf node)
          // So let find the child node where the key is supposed to belong
          i = 0;
          int numberOfKeys = rootNode.mCurrentKeyNum;
          A currentKey = rootNode.mKeys[i];
          while ((i < numberOfKeys) && (key.compareTo(currentKey) > 0)) {
              ++i;
              if (i < numberOfKeys) {
                  currentKey = rootNode.mKeys[i];
              }
              else {
                  --i;
                  break;
              }
          }
  
          if ((i < numberOfKeys) && (key.compareTo(currentKey) == 0)) {
              // The key already existed so replace its value and done with it
              currentKey.fetchValue(key);
              --mSize;
              return;
          }
  
          IndexPage<A,B> btNode;
          if (key.compareTo(currentKey) > 0) {
              btNode = BaseIndexRoot.getRightChildAtIndex(rootNode, i);
              i = i + 1;
          }
          else {
              if ((i - 1 >= 0) && (key.compareTo(rootNode.mKeys[i - 1]) > 0)) {
                  btNode = BaseIndexRoot.getRightChildAtIndex(rootNode, i - 1);
              }
              else {
                  btNode = BaseIndexRoot.getLeftChildAtIndex(rootNode, i);
              }
          }
  
          if (btNode.mCurrentKeyNum == root.UPPER_BOUND_KEYNUM()) {
              // If the child node is a full node then handle it by splitting out
              // then insert key starting at the root node after splitting node
              splitNode(rootNode, i, btNode);
              insertKeyAtNode(rootNode, key);
              return;
          }
  
          insertKeyAtNode(btNode, key);
      }
  
  
      //
      // Split a child with respect to its parent at a specified node
      //
      private void splitNode(IndexPage<A,B> parentNode, int nodeIdx, IndexPage<A,B> btNode) throws InternalServerException {
          int i;
  
          IndexPage<A,B> newNode = createNode();
          parentNode.changed = true;
          btNode.changed = true;
  
          newNode.mIsLeaf = btNode.mIsLeaf;
  
          // Since the node is full,
          // new node must share LOWER_BOUND_KEYNUM (aka t - 1) keys from the node
          newNode.mCurrentKeyNum = root.LOWER_BOUND_KEYNUM();
  
          final int MIN_DEGREE = root.MIN_DEGREE(); 
          // Copy right half of the keys from the node to the new node
          for (i = 0; i < root.LOWER_BOUND_KEYNUM(); ++i) {
              newNode.mKeys[i] = btNode.mKeys[i + MIN_DEGREE];
              btNode.mKeys[i + MIN_DEGREE] = null;
          }
  
          // If the node is an internal node (not a leaf),
          // copy the its child pointers at the half right as well
          if (!btNode.mIsLeaf) {
              for (i = 0; i < MIN_DEGREE; ++i) {
                  newNode.mChildren[i] = btNode.mChildren[i + MIN_DEGREE];
                  btNode.mChildren[i + MIN_DEGREE] = null;
              }
          }
  
          // The node at this point should have LOWER_BOUND_KEYNUM (aka min degree - 1) keys at this point.
          // We will move its right-most key to its parent node later.
          btNode.mCurrentKeyNum = root.LOWER_BOUND_KEYNUM();
  
          // Do the right shift for relevant child pointers of the parent node
          // so that we can put the new node as its new child pointer
          for (i = parentNode.mCurrentKeyNum; i > nodeIdx; --i) {
              parentNode.mChildren[i + 1] = parentNode.mChildren[i];
              parentNode.mChildren[i] = null;
          }
          parentNode.mChildren[nodeIdx + 1] = newNode.getId();
  
          // Do the right shift all the keys of the parent node the right side of the node index as well
          // so that we will have a slot for move a median key from the splitted node
          for (i = parentNode.mCurrentKeyNum - 1; i >= nodeIdx; --i) {
              parentNode.mKeys[i + 1] = parentNode.mKeys[i];
              parentNode.mKeys[i] = null;
          }
          parentNode.mKeys[nodeIdx] = btNode.mKeys[root.LOWER_BOUND_KEYNUM()];
          btNode.mKeys[root.LOWER_BOUND_KEYNUM()] = null;
          ++(parentNode.mCurrentKeyNum);
      }
  
  
      //
      // Find the predecessor node for a specified node
      //
      private IndexPage<A,B> findPredecessor(IndexPage<A,B> btNode, int nodeIdx) throws InternalServerException, LostUpdateException {
          if (btNode.mIsLeaf) {
              return btNode;
          }
  
          IndexPage<A,B> predecessorNode;
          if (nodeIdx > -1) {
              predecessorNode = BaseIndexRoot.getLeftChildAtIndex(btNode, nodeIdx);
              if (predecessorNode != null) {
                  mIntermediateInternalNode = btNode;
                  mNodeIdx = nodeIdx;
                  btNode = findPredecessor(predecessorNode, -1);
              }
  
              return btNode;
          }
  
          predecessorNode = BaseIndexRoot.getRightChildAtIndex(btNode, btNode.mCurrentKeyNum - 1);
          if (predecessorNode != null) {
              mIntermediateInternalNode = btNode;
              mNodeIdx = btNode.mCurrentKeyNum;
              btNode = findPredecessorForNode(predecessorNode, -1);
          }
  
          return btNode;
      }
  
  
      //
      // Find predecessor node of a specified node
      //
      private IndexPage<A,B> findPredecessorForNode(IndexPage<A,B> btNode, int keyIdx) throws InternalServerException, LostUpdateException {
          IndexPage<A,B> predecessorNode;
          IndexPage<A,B> originalNode = btNode;
          if (keyIdx > -1) {
              predecessorNode = BaseIndexRoot.getLeftChildAtIndex(btNode, keyIdx);
              if (predecessorNode != null) {
                  btNode = findPredecessorForNode(predecessorNode, -1);
                  rebalanceTreeAtNode(originalNode, predecessorNode, keyIdx, REBALANCE_FOR_LEAF_NODE);
              }
  
              return btNode;
          }
  
          predecessorNode = BaseIndexRoot.getRightChildAtIndex(btNode, btNode.mCurrentKeyNum - 1);
          if (predecessorNode != null) {
              btNode = findPredecessorForNode(predecessorNode, -1);
              rebalanceTreeAtNode(originalNode, predecessorNode, keyIdx, REBALANCE_FOR_LEAF_NODE);
          }
  
          return btNode;
      }
  
  
      //
      // Do the left rotation
      //
      private void performLeftRotation(IndexPage<A,B> btNode, int nodeIdx, IndexPage<A,B> parentNode, IndexPage<A,B> rightSiblingNode) throws InternalServerException  {
          int parentKeyIdx = nodeIdx;
  
          parentNode.changed = true;
          rightSiblingNode.changed = true;
          btNode.changed = true;
          /*
          if (nodeIdx >= parentNode.mCurrentKeyNum) {
              // This shouldn't happen
              parentKeyIdx = nodeIdx - 1;
          }
          */
  
          // Move the parent key and relevant child to the deficient node
          btNode.mKeys[btNode.mCurrentKeyNum] = parentNode.mKeys[parentKeyIdx];
          btNode.mChildren[btNode.mCurrentKeyNum + 1] = rightSiblingNode.mChildren[0];
          ++(btNode.mCurrentKeyNum);
  
          // Move the leftmost key of the right sibling and relevant child pointer to the parent node
          parentNode.mKeys[parentKeyIdx] = rightSiblingNode.mKeys[0];
          --(rightSiblingNode.mCurrentKeyNum);
          // Shift all keys and children of the right sibling to its left
          for (int i = 0; i < rightSiblingNode.mCurrentKeyNum; ++i) {
              rightSiblingNode.mKeys[i] = rightSiblingNode.mKeys[i + 1];
              rightSiblingNode.mChildren[i] = rightSiblingNode.mChildren[i + 1];
          }
          rightSiblingNode.mChildren[rightSiblingNode.mCurrentKeyNum] = rightSiblingNode.mChildren[rightSiblingNode.mCurrentKeyNum + 1];
          rightSiblingNode.mChildren[rightSiblingNode.mCurrentKeyNum + 1] = null;
      }
  
  
      //
      // Do the right rotation
      //
      private void performRightRotation(IndexPage<A,B> btNode, int nodeIdx, IndexPage<A,B> parentNode, IndexPage<A,B> leftSiblingNode) {
          int parentKeyIdx = nodeIdx;
          if (nodeIdx >= parentNode.mCurrentKeyNum) {
              // This shouldn't happen
              parentKeyIdx = nodeIdx - 1;
          }
          
          parentNode.changed = true;
          leftSiblingNode.changed = true;
          btNode.changed = true;
  
          // Shift all keys and children of the deficient node to the right
          // So that there will be available left slot for insertion
          btNode.mChildren[btNode.mCurrentKeyNum + 1] = btNode.mChildren[btNode.mCurrentKeyNum];
          for (int i = btNode.mCurrentKeyNum - 1; i >= 0; --i) {
              btNode.mKeys[i + 1] = btNode.mKeys[i];
              btNode.mChildren[i + 1] = btNode.mChildren[i];
          }
  
          // Move the parent key and relevant child to the deficient node
          btNode.mKeys[0] = parentNode.mKeys[parentKeyIdx];
          btNode.mChildren[0] = leftSiblingNode.mChildren[leftSiblingNode.mCurrentKeyNum];
          ++(btNode.mCurrentKeyNum);
  
          // Move the leftmost key of the right sibling and relevant child pointer to the parent node
          parentNode.mKeys[parentKeyIdx] = leftSiblingNode.mKeys[leftSiblingNode.mCurrentKeyNum - 1];
          leftSiblingNode.mChildren[leftSiblingNode.mCurrentKeyNum] = null;
          --(leftSiblingNode.mCurrentKeyNum);
      }
  
  
      //
      // Do a left sibling merge
      // Return true if it should continue further
      // Return false if it is done
      //
      private boolean performMergeWithLeftSibling(IndexPage<A,B> btNode, int nodeIdx, IndexPage<A,B> parentNode, IndexPage<A,B> leftSiblingNode) {
    	  btNode.changed = true;
    	  parentNode.changed = true;
    	  leftSiblingNode.changed = true;
    	  
          if (nodeIdx == parentNode.mCurrentKeyNum) {
              // For the case that the node index can be the right most
              nodeIdx = nodeIdx - 1;
          }
  
          // Here we need to determine the parent node's index based on child node's index (nodeIdx)
          if (nodeIdx > 0) {
              if (leftSiblingNode.mKeys[leftSiblingNode.mCurrentKeyNum - 1].compareTo(parentNode.mKeys[nodeIdx - 1]) < 0) {
                  nodeIdx = nodeIdx - 1;
              }
          }
  
          // Copy the parent key to the node (on the left)
          leftSiblingNode.mKeys[leftSiblingNode.mCurrentKeyNum] = parentNode.mKeys[nodeIdx];
          ++(leftSiblingNode.mCurrentKeyNum);
  
          // Copy keys and children of the node to the left sibling node
          for (int i = 0; i < btNode.mCurrentKeyNum; ++i) {
              leftSiblingNode.mKeys[leftSiblingNode.mCurrentKeyNum + i] = btNode.mKeys[i];
              leftSiblingNode.mChildren[leftSiblingNode.mCurrentKeyNum + i] = btNode.mChildren[i];
              btNode.mKeys[i] = null;
          }
          leftSiblingNode.mCurrentKeyNum += btNode.mCurrentKeyNum;
          leftSiblingNode.mChildren[leftSiblingNode.mCurrentKeyNum] = btNode.mChildren[btNode.mCurrentKeyNum];
          btNode.mCurrentKeyNum = 0;  // Abandon the node
  
          // Shift all relevant keys and children of the parent node to the left
          // since it lost one of its keys and children (by moving it to the child node)
          int i;
          for (i = nodeIdx; i < parentNode.mCurrentKeyNum - 1; ++i) {
              parentNode.mKeys[i] = parentNode.mKeys[i + 1];
              parentNode.mChildren[i + 1] = parentNode.mChildren[i + 2];
          }
          parentNode.mKeys[i] = null;
          parentNode.mChildren[parentNode.mCurrentKeyNum] = null;
          --(parentNode.mCurrentKeyNum);
  
          // Make sure the parent point to the correct child after the merge
          parentNode.mChildren[nodeIdx] = leftSiblingNode.getId();
  
          if ((parentNode == mRoot) && (parentNode.mCurrentKeyNum == 0)) {
              // Root node is updated.  It should be done
              mRoot.copyFrom(leftSiblingNode);
              return false;
          }
  
          return true;
      }
  
  
      //
      // Do the right sibling merge
      // Return true if it should continue further
      // Return false if it is done
      //
      private boolean performMergeWithRightSibling(IndexPage<A,B> btNode, int nodeIdx, IndexPage<A,B> parentNode, IndexPage<A,B> rightSiblingNode) {
    	  
    	  btNode.changed = true;
    	  parentNode.changed = true;
    	  rightSiblingNode.changed = true;
    	  
          // Copy the parent key to right-most slot of the node
          btNode.mKeys[btNode.mCurrentKeyNum] = parentNode.mKeys[nodeIdx];
          ++(btNode.mCurrentKeyNum);
  
          // Copy keys and children of the right sibling to the node
          for (int i = 0; i < rightSiblingNode.mCurrentKeyNum; ++i) {
              btNode.mKeys[btNode.mCurrentKeyNum + i] = rightSiblingNode.mKeys[i];
              btNode.mChildren[btNode.mCurrentKeyNum + i] = rightSiblingNode.mChildren[i];
          }
          btNode.mCurrentKeyNum += rightSiblingNode.mCurrentKeyNum;
          btNode.mChildren[btNode.mCurrentKeyNum] = rightSiblingNode.mChildren[rightSiblingNode.mCurrentKeyNum];
          rightSiblingNode.mCurrentKeyNum = 0;  // Abandon the sibling node
  
          // Shift all relevant keys and children of the parent node to the left
          // since it lost one of its keys and children (by moving it to the child node)
          int i;
          for (i = nodeIdx; i < parentNode.mCurrentKeyNum - 1; ++i) {
              parentNode.mKeys[i] = parentNode.mKeys[i + 1];
              parentNode.mChildren[i + 1] = parentNode.mChildren[i + 2];
          }
          parentNode.mKeys[i] = null;
          parentNode.mChildren[parentNode.mCurrentKeyNum] = null;
          --(parentNode.mCurrentKeyNum);
  
          // Make sure the parent point to the correct child after the merge
          parentNode.mChildren[nodeIdx] = btNode.getId();
  
          if ((parentNode == mRoot) && (parentNode.mCurrentKeyNum == 0)) {
              // Root node is updated.  It should be done
              mRoot.copyFrom(btNode);
              return false;
          }
  
          return true;
      }
  
  
      //
      // Search the specified key within a node
      // Return index of the keys if it finds
      // Return -1 otherwise
      //
      private int searchKey(IndexPage<A,B> btNode, A key) {
          for (int i = 0; i < btNode.mCurrentKeyNum; ++i) {
              if (key.compareTo(btNode.mKeys[i]) == 0) {
                  return i;
              }
              else if (key.compareTo(btNode.mKeys[i]) < 0) {
                  return -1;
              }
          }
  
          return -1;
      }
  
  
  
      //
      // Delete a key from the tree
      // Return value if it finds the key and delete it
      // Return null if it cannot find the key
      //
      public A delete(A key) throws InternalServerException, LostUpdateException {
          mIntermediateInternalNode = null;
          A keyVal = deleteKey(null, mRoot, key, 0);
          if (keyVal == null) {
              return null;
          }
          --mSize;
          return keyVal;
      }
  
  
      //
      // Delete a key from a tree node
      //
      private A deleteKey(IndexPage<A,B> parentNode, IndexPage<A,B> btNode, A key, int nodeIdx) throws InternalServerException, LostUpdateException {
          int i;
          int nIdx;
          A retVal;
  
          if (btNode == null) {
              // The tree is empty
              return null;
          }
  
          if (btNode.mIsLeaf) {
              nIdx = searchKey(btNode, key);
              if (nIdx < 0) {
                  // Can't find the specified key
                  return null;
              }
  
              retVal = btNode.mKeys[nIdx];
  
              if ((btNode.mCurrentKeyNum > root.LOWER_BOUND_KEYNUM()) || (parentNode == null)) {
                  // Remove it from the node
                  for (i = nIdx; i < btNode.mCurrentKeyNum - 1; ++i) {
                      btNode.mKeys[i] = btNode.mKeys[i + 1];
                  }
                  btNode.mKeys[i] = null;
                  --(btNode.mCurrentKeyNum);
                  btNode.changed = true;
  
                  /*if (btNode.mCurrentKeyNum == 0) {
                      // btNode is actually the root node
                      mRoot = null;
                  }*/
  
                  return retVal;
              }
  
              // Find the left sibling
              IndexPage<A,B> rightSibling;
              IndexPage<A,B> leftSibling = BaseIndexRoot.getLeftSiblingAtIndex(parentNode, nodeIdx);
              if ((leftSibling != null) && (leftSibling.mCurrentKeyNum > root.LOWER_BOUND_KEYNUM())) {
                  // Remove the key and borrow a key from the left sibling
                  moveLeftLeafSiblingKeyWithKeyRemoval(btNode, nodeIdx, nIdx, parentNode, leftSibling);
              }
              else {
                  rightSibling = BaseIndexRoot.getRightSiblingAtIndex(parentNode, nodeIdx);
                  if ((rightSibling != null) && (rightSibling.mCurrentKeyNum > root.LOWER_BOUND_KEYNUM())) {
                      // Remove a key and borrow a key the right sibling
                      moveRightLeafSiblingKeyWithKeyRemoval(btNode, nodeIdx, nIdx, parentNode, rightSibling);
                  }
                  else {
                      // Merge to its sibling
                      boolean isRebalanceNeeded = false;
                      boolean bStatus;
                      if (leftSibling != null) {
                          // Merge with the left sibling
                          bStatus = doLeafSiblingMergeWithKeyRemoval(btNode, nodeIdx, nIdx, parentNode, leftSibling, false);
                          if (!bStatus) {
                              isRebalanceNeeded = false;
                          }
                          else if (parentNode.mCurrentKeyNum < root.LOWER_BOUND_KEYNUM()) {
                              // Need to rebalance the tree
                              isRebalanceNeeded = true;
                          }
                      }
                      else {
                          // Merge with the right sibling
                          bStatus = doLeafSiblingMergeWithKeyRemoval(btNode, nodeIdx, nIdx, parentNode, rightSibling, true);
                          if (!bStatus) {
                              isRebalanceNeeded = false;
                          }
                          else if (parentNode.mCurrentKeyNum < root.LOWER_BOUND_KEYNUM()) {
                              // Need to rebalance the tree
                              isRebalanceNeeded = true;
                          }
                      }
  
                      if (isRebalanceNeeded && (mRoot != null)) {
                          rebalanceTree(mRoot, parentNode, parentNode.mKeys[0]);
                      }
                  }
              }
  
              return retVal;  // Done with handling for the leaf node
          }
  
          //
          // At this point the node is an internal node
          //
  
          nIdx = searchKey(btNode, key);
          if (nIdx >= 0) {
              // We found the key in the internal node
  
              // Find its predecessor
              mIntermediateInternalNode = btNode;
              mNodeIdx = nIdx;
              IndexPage<A,B> predecessorNode =  findPredecessor(btNode, nIdx);
              A predecessorKey = predecessorNode.mKeys[predecessorNode.mCurrentKeyNum - 1];
  
              // Swap the data of the deleted key and its predecessor (in the leaf node)
              A deletedKey = btNode.mKeys[nIdx];
              btNode.mKeys[nIdx] = predecessorKey;
              predecessorNode.mKeys[predecessorNode.mCurrentKeyNum - 1] = deletedKey;
  
              btNode.changed = true;
              predecessorNode.changed = true;
              // mIntermediateNode is done in findPrecessor
              return deleteKey(mIntermediateInternalNode, predecessorNode, deletedKey, mNodeIdx);
          }
  
          //
          // Find the child subtree (node) that contains the key
          //
          i = 0;
          A currentKey = btNode.mKeys[0];
          while ((i < btNode.mCurrentKeyNum) && (key.compareTo(currentKey) > 0)) {
              ++i;
              if (i < btNode.mCurrentKeyNum) {
                  currentKey = btNode.mKeys[i];
              }
              else {
                  --i;
                  break;
              }
          }
  
          IndexPage<A,B> childNode;
          if (key.compareTo(currentKey) > 0) {
              childNode = BaseIndexRoot.getRightChildAtIndex(btNode, i);
              if (childNode.mKeys[0].compareTo(btNode.mKeys[btNode.mCurrentKeyNum - 1]) > 0) {
                  // The right-most side of the node
                  i = i + 1;
              }
          }
          else {
              childNode = BaseIndexRoot.getLeftChildAtIndex(btNode, i);
          }
  
          return deleteKey(btNode, childNode, key, i);
      }
  
  
      //
      // Remove the specified key and move a key from the right leaf sibling to the node
      // Note: The node and its sibling must be leaves
      //
      private void moveRightLeafSiblingKeyWithKeyRemoval(IndexPage<A,B> btNode,
                                                         int nodeIdx,
                                                         int keyIdx,
                                                         IndexPage<A,B> parentNode,
                                                         IndexPage<A,B> rightSiblingNode) {
          btNode.changed = true;
          parentNode.changed = true;
          rightSiblingNode.changed = true;
          
    	  // Shift to the right where the key is deleted
          for (int i = keyIdx; i < btNode.mCurrentKeyNum - 1; ++i) {
              btNode.mKeys[i] = btNode.mKeys[i + 1];
          }
  
          btNode.mKeys[btNode.mCurrentKeyNum - 1] = parentNode.mKeys[nodeIdx];
          parentNode.mKeys[nodeIdx] = rightSiblingNode.mKeys[0];
  
          for (int i = 0; i < rightSiblingNode.mCurrentKeyNum - 1; ++i) {
              rightSiblingNode.mKeys[i] = rightSiblingNode.mKeys[i + 1];
          }
  
          --(rightSiblingNode.mCurrentKeyNum);
      }
  
  
      //
      // Remove the specified key and move a key from the left leaf sibling to the node
      // Note: The node and its sibling must be leaves
      //
      private void moveLeftLeafSiblingKeyWithKeyRemoval(IndexPage<A,B> btNode,
                                                        int nodeIdx,
                                                        int keyIdx,
                                                        IndexPage<A,B> parentNode,
                                                        IndexPage<A,B> leftSiblingNode) {
    	  
    	  btNode.changed = true;
    	  leftSiblingNode.changed = true;
    	  parentNode.changed = true;
    	  
          // Use the parent key on the left side of the node
          nodeIdx = nodeIdx - 1;
  
          // Shift to the right to where the key will be deleted 
          for (int i = keyIdx; i > 0; --i) {
              btNode.mKeys[i] = btNode.mKeys[i - 1];
          }
  
          btNode.mKeys[0] = parentNode.mKeys[nodeIdx];
          parentNode.mKeys[nodeIdx] = leftSiblingNode.mKeys[leftSiblingNode.mCurrentKeyNum - 1];
          --(leftSiblingNode.mCurrentKeyNum);
      }
  
  
      //
      // Do the leaf sibling merge
      // Return true if we need to perform futher re-balancing action
      // Return false if we reach and update the root hence we don't need to go futher for re-balancing the tree
      //
      private boolean doLeafSiblingMergeWithKeyRemoval(IndexPage<A,B> btNode,
                                                       int nodeIdx,
                                                       int keyIdx,
                                                       IndexPage<A,B> parentNode,
                                                       IndexPage<A,B> siblingNode,
                                                       boolean isRightSibling) throws InternalServerException, LostUpdateException {
          int i;
          
          btNode.changed = true;
          parentNode.changed = true;
          siblingNode.changed = true;
  
          if (nodeIdx == parentNode.mCurrentKeyNum) {
              // Case node index can be the right most
              nodeIdx = nodeIdx - 1;
          }
  
          if (isRightSibling) {
              // Shift the remained keys of the node to the left to remove the key
              for (i = keyIdx; i < btNode.mCurrentKeyNum - 1; ++i) {
                  btNode.mKeys[i] = btNode.mKeys[i + 1];
              }
              btNode.mKeys[i] = parentNode.mKeys[nodeIdx];
          }
          else {
              // Here we need to determine the parent node id based on child node id (nodeIdx)
              if (nodeIdx > 0) {
                  if (siblingNode.mKeys[siblingNode.mCurrentKeyNum - 1].compareTo(parentNode.mKeys[nodeIdx - 1]) < 0) {
                      nodeIdx = nodeIdx - 1;
                  }
              }
  
              siblingNode.mKeys[siblingNode.mCurrentKeyNum] = parentNode.mKeys[nodeIdx];
              // siblingNode.mKeys[siblingNode.mCurrentKeyNum] = parentNode.mKeys[0];
              ++(siblingNode.mCurrentKeyNum);
  
              // Shift the remained keys of the node to the left to remove the key
              for (i = keyIdx; i < btNode.mCurrentKeyNum - 1; ++i) {
                  btNode.mKeys[i] = btNode.mKeys[i + 1];
              }
              btNode.mKeys[i] = null;
              --(btNode.mCurrentKeyNum);
          }
  
          if (isRightSibling) {
              for (i = 0; i < siblingNode.mCurrentKeyNum; ++i) {
                  btNode.mKeys[btNode.mCurrentKeyNum + i] = siblingNode.mKeys[i];
                  siblingNode.mKeys[i] = null;
              }
              btNode.mCurrentKeyNum += siblingNode.mCurrentKeyNum;
              siblingNode.mCurrentKeyNum = 0;
          }
          else {
              for (i = 0; i < btNode.mCurrentKeyNum; ++i) {
                  siblingNode.mKeys[siblingNode.mCurrentKeyNum + i] = btNode.mKeys[i];
                  btNode.mKeys[i] = null;
              }
              siblingNode.mCurrentKeyNum += btNode.mCurrentKeyNum;
              btNode.mKeys[btNode.mCurrentKeyNum] = null;
              btNode.mCurrentKeyNum = 0;
          }
  
          // Shift the parent keys accordingly after the merge of child nodes
          for (i = nodeIdx; i < parentNode.mCurrentKeyNum - 1; ++i) {
              parentNode.mKeys[i] = parentNode.mKeys[i + 1];
              parentNode.mChildren[i + 1] = parentNode.mChildren[i + 2];
          }
          parentNode.mKeys[i] = null;
          parentNode.mChildren[parentNode.mCurrentKeyNum] = null;
          --(parentNode.mCurrentKeyNum);
  
          if (isRightSibling) {
              parentNode.mChildren[nodeIdx] = btNode.getId();
          }
          else {
              parentNode.mChildren[nodeIdx] = siblingNode.getId();
          }
  
          if ((mRoot == parentNode) && (mRoot.mCurrentKeyNum == 0)) {
              // Only root left
              mRoot.copyFrom(parentNode.getChild(nodeIdx));
              mRoot.mIsLeaf = true;
              return false;  // Root has been changed, we don't need to go futher
          }
  
          return true;
      }
  
  
      //
      // Re-balance the tree at a specified node
      // Params:
      // parentNode = the parent node of the node needs to be re-balanced
      // btNode = the node needs to be re-balanced
      // nodeIdx = the index of the parent node's child array where the node belongs
      // balanceType = either REBALANCE_FOR_LEAF_NODE or REBALANCE_FOR_INTERNAL_NODE
      //   REBALANCE_FOR_LEAF_NODE: the node is a leaf
      //   REBALANCE_FOR_INTERNAL_NODE: the node is an internal node
      // Return:
      // true if it needs to continue rebalancing further
      // false if further rebalancing is no longer needed
      //
      private boolean rebalanceTreeAtNode(IndexPage<A,B> parentNode, IndexPage<A,B> btNode, int nodeIdx, int balanceType) throws InternalServerException, LostUpdateException {
          if (balanceType == REBALANCE_FOR_LEAF_NODE) {
              if ((btNode == null) || (btNode == mRoot)) {
                  return false;
              }
          }
          else if (balanceType == REBALANCE_FOR_INTERNAL_NODE) {
              if (parentNode == null) {
                  // Root node
                  return false;
              }
          }
  
          if (btNode.mCurrentKeyNum >= root.LOWER_BOUND_KEYNUM()) {
              // The node doesn't need to rebalance
              return false;
          }
  
          IndexPage<A,B> rightSiblingNode;
          IndexPage<A,B> leftSiblingNode = BaseIndexRoot.getLeftSiblingAtIndex(parentNode, nodeIdx);
          if ((leftSiblingNode != null) && (leftSiblingNode.mCurrentKeyNum > root.LOWER_BOUND_KEYNUM())) {
              // Do right rotate
              performRightRotation(btNode, nodeIdx, parentNode, leftSiblingNode);
          }
          else {
              rightSiblingNode = BaseIndexRoot.getRightSiblingAtIndex(parentNode, nodeIdx);
              if ((rightSiblingNode != null) && (rightSiblingNode.mCurrentKeyNum > root.LOWER_BOUND_KEYNUM())) {
                  // Do left rotate
                  performLeftRotation(btNode, nodeIdx, parentNode, rightSiblingNode);
              }
              else {
                  // Merge the node with one of the siblings
                  boolean bStatus;
                  if (leftSiblingNode != null) {
                      bStatus = performMergeWithLeftSibling(btNode, nodeIdx, parentNode, leftSiblingNode);
                  }
                  else {
                      bStatus = performMergeWithRightSibling(btNode, nodeIdx, parentNode, rightSiblingNode);
                  }
  
                  if (!bStatus) {
                      return false;
                  }
              }
          }
  
          return true;
      }
  
  
      //
      // Re-balance the tree upward from the lower node to the upper node
      //
      private void rebalanceTree(IndexPage<A,B> upperNode, IndexPage<A,B> lowerNode, A key) throws InternalServerException, LostUpdateException {
          mStackTracer.clear();
          mStackTracer.add(new StackInfo(null, upperNode, 0));
  
          //
          // Find the child subtree (node) that contains the key
          //
          IndexPage<A,B> parentNode, childNode;
          A currentKey;
          int i;
          parentNode = upperNode;
          while ((parentNode != lowerNode) && !parentNode.mIsLeaf) {
              currentKey = parentNode.mKeys[0];
              i = 0;
              while ((i < parentNode.mCurrentKeyNum) && (key.compareTo(currentKey) > 0)) {
                  ++i;
                  if (i < parentNode.mCurrentKeyNum) {
                      currentKey = parentNode.mKeys[i];
                  }
                  else {
                      --i;
                      break;
                  }
              }
  
              if (key.compareTo(currentKey) > 0) {
                  childNode = BaseIndexRoot.getRightChildAtIndex(parentNode, i);
                  if (childNode.mKeys[0].compareTo(parentNode.mKeys[parentNode.mCurrentKeyNum - 1]) > 0) {
                      // The right-most side of the node
                      i = i + 1;
                  }
              }
              else {
                  childNode = BaseIndexRoot.getLeftChildAtIndex(parentNode, i);
              }
  
              if (childNode == null) {
                  break;
              }
  
              if (key.compareTo(currentKey) == 0) {
                  break;
              }
  
              mStackTracer.add(new StackInfo(parentNode, childNode, i));
              parentNode = childNode;
          }
  
          boolean bStatus;
          StackInfo stackInfo;
          while (!mStackTracer.isEmpty()) {
              stackInfo = mStackTracer.pop();
              if ((stackInfo != null) && !stackInfo.mNode.mIsLeaf) {
                  bStatus = rebalanceTreeAtNode(stackInfo.mParent,
                                                stackInfo.mNode,
                                                stackInfo.mNodeIdx,
                                                REBALANCE_FOR_INTERNAL_NODE);
                  if (!bStatus) {
                      break;
                  }
              }
          }
      }
  
  
      /**
       * Inner class StackInfo for tracing-back purpose
       * Structure contains parent node and node index
       */
      public class StackInfo {
          public IndexPage<A,B> mParent = null;
          public IndexPage<A,B> mNode = null;
          public int mNodeIdx = -1;
  
          public StackInfo(IndexPage<A,B> parent, IndexPage<A,B> node, int nodeIdx) {
              mParent = parent;
              mNode = node;
              mNodeIdx = nodeIdx;
          }
      }
  }