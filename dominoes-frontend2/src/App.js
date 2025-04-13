import styled from 'styled-components';
import Board from './components/Board';
import React, { useState, useRef, useEffect  } from 'react';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import Bank from './components/Bank';

const AppContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  font-family: Arial, sans-serif;
`;

const BoardContainer = styled.div`
  position: relative;
  width: 80vw;
  height: 80vh;
  border: 2px solid black;
  margin-bottom: 20px;
  border-radius: 30px;
  overflow: hidden; /* Add this to contain the sliding bank */
`;



function App() {

  const transitionSpeed = 0.5;

  const tileSet = [
    { side1: 0, side2: 0, id: '0-0' },
    { side1: 0, side2: 1, id: '0-1' },
    { side1: 0, side2: 2, id: '0-2' },
    { side1: 0, side2: 3, id: '0-3' },
    { side1: 0, side2: 4, id: '0-4' },
    { side1: 0, side2: 5, id: '0-5' },
    { side1: 0, side2: 6, id: '0-6' },
    { side1: 1, side2: 1, id: '1-1' },
    { side1: 1, side2: 2, id: '1-2' },
    { side1: 1, side2: 3, id: '1-3' },
    { side1: 1, side2: 4, id: '1-4' },
    { side1: 1, side2: 5, id: '1-5' },
    { side1: 1, side2: 6, id: '1-6' },
    { side1: 2, side2: 2, id: '2-2' },
    { side1: 2, side2: 3, id: '2-3' },
    { side1: 2, side2: 4, id: '2-4' },
    { side1: 2, side2: 5, id: '2-5' },
    { side1: 2, side2: 6, id: '2-6' },
    { side1: 3, side2: 3, id: '3-3' },
    { side1: 3, side2: 4, id: '3-4' },
    { side1: 3, side2: 5, id: '3-5' },
    { side1: 3, side2: 6, id: '3-6' },
    { side1: 4, side2: 4, id: '4-4' },
    { side1: 4, side2: 5, id: '4-5' },
    { side1: 4, side2: 6, id: '4-6' },
    { side1: 5, side2: 5, id: '5-5' },
    { side1: 5, side2: 6, id: '5-6' },
    { side1: 6, side2: 6, id: '6-6' },
  ];

  let [bank, setBank] = useState(tileSet);
  const [hand, setHand] = useState([]);

  // In App.js, add these new state and functions
  const [boardTiles, setBoardTiles] = useState([]);

  const handleDragFromBoard = (tile) => {
    // Don't remove immediately - wait to see where it's dropped
    // The actual removal will happen in handleDropOnBank or handleDropOnHand
    return tile;
  };

  // In App.js, add this state
  const [draggedTile, setDraggedTile] = useState(null);

  // Then modify the handlers:

  const handleDragStart = (tile) => {
    setDraggedTile(tile);
    return tile;
  };

  const handleDropOnBoard = (tile) => {
    if (!draggedTile) return;
    
    // Check if tile is already on board
    if (boardTiles.some(t => t.id === draggedTile.id)) return;

    const newTile = { ...draggedTile };
    const currentBoard = [...boardTiles];

    if (currentBoard.length === 0) {
      setBoardTiles([{ ...newTile, flipped: false }]);
    } else {
      const firstValue = currentBoard[0].side1;
      const lastValue = currentBoard[currentBoard.length - 1].side2;

      // Check for end matches
      if (newTile.side1 === lastValue || newTile.side2 === lastValue) {
        const flipped = newTile.side1 !== lastValue;
        setBoardTiles([...currentBoard, { ...newTile, flipped }]);
      } 
      // Check for beginning matches
      else if (newTile.side1 === firstValue || newTile.side2 === firstValue) {
        const flipped = newTile.side2 !== firstValue;
        setBoardTiles([{ ...newTile, flipped }, ...currentBoard]);
      } else {
        return; // No valid placement
      }
    }

    // Remove from source
    if (bank.some(t => t.id === draggedTile.id)) {
      setBank(prev => prev.filter(t => t.id !== draggedTile.id));
    } else if (hand.some(t => t.id === draggedTile.id)) {
      setHand(prev => prev.filter(t => t.id !== draggedTile.id));
    }
    
    setDraggedTile(null);
  };

  const handleDropOnBank = (tile) => {
    if (!draggedTile) return;
    
    // Allow from hand or board
    const fromHand = hand.some(t => t.id === draggedTile.id);
    const fromBoard = boardTiles.some(t => t.id === draggedTile.id);
    
    if (!fromHand && !fromBoard) return;
    
    setBank(prevBank => {
      if (prevBank.some(t => t.id === draggedTile.id)) return prevBank;
      
      const newTile = {...draggedTile};
      if (newTile.side1 > newTile.side2) {
        [newTile.side1, newTile.side2] = [newTile.side2, newTile.side1];
      }
      return [...prevBank, newTile].sort(compareTiles);
    });

    // Remove from source
    if (fromHand) {
      setHand(prev => prev.filter(t => t.id !== draggedTile.id));
    } else if (fromBoard) {
      setBoardTiles(prev => prev.filter(t => t.id !== draggedTile.id));
    }
    
    setDraggedTile(null);
  };

  const handleDropOnHand = (tile) => {
    if (!draggedTile) return;
    
    // Allow from bank or board
    const fromBank = bank.some(t => t.id === draggedTile.id);
    const fromBoard = boardTiles.some(t => t.id === draggedTile.id);
    
    if (!fromBank && !fromBoard) return;
    
    setHand(prevHand => {
      if (prevHand.some(t => t.id === draggedTile.id)) return prevHand;
      
      const newTile = {...draggedTile};
      if (newTile.side1 > newTile.side2) {
        [newTile.side1, newTile.side2] = [newTile.side2, newTile.side1];
      }
      return [...prevHand, newTile].sort(compareTiles);
    });

    // Remove from source
    if (fromBank) {
      setBank(prev => prev.filter(t => t.id !== draggedTile.id));
    } else if (fromBoard) {
      setBoardTiles(prev => prev.filter(t => t.id !== draggedTile.id));
    }
    
    setDraggedTile(null);
  };

  function compareTiles(a, b){
    const aVal = a.side1 * 10 + a.side2;
    const bVal = b.side1 * 10 + b.side2;
  
    return aVal - bVal;
  }


  return(
    <DndProvider backend={HTML5Backend}>
      <AppContainer>

        <h1>Dominoes Solver</h1>

        <BoardContainer>
          <Board 
            bank={bank} 
            setBank={setBank}
            hand={hand}
            setHand={setHand}
            boardTiles={boardTiles}
            handleDropOnBank={handleDropOnBank}
            handleDropOnHand={handleDropOnHand}
            handleDropOnBoard={handleDropOnBoard}
            handleDragFromBoard={handleDragFromBoard}
            speed={transitionSpeed}
          />
        </BoardContainer>

      </AppContainer>
    </DndProvider>
  );
};

export default App;
