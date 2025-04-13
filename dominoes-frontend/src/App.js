import React, { useState, useRef, useEffect  } from 'react';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import styled from 'styled-components';
import Board from './components/Board';
import Bank from './components/Bank';
import PlayerHand from './components/PlayerHand';
import ModelPrediction from './components/ModelPrediction';
import MousePositionTracker, {position} from './components/MousePositionTracker';
import axios from 'axios';

const AppContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  font-family: Arial, sans-serif;
`;

const BoardContainer = styled.div`
  position: relative;
  width: 80%;
  height: 300px;
  border: 2px solid black;
  margin-bottom: 20px;
`;

const UndoButton = styled.button`
  position: absolute;
  top: 10px;
  left: 10px;
`;

const BankContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
`;

const BankLabel = styled.div`
  font-weight: bold;
  margin-bottom: 10px;
`;

const HandAndPrediction = styled.div`
  display: flex;
  justify-content: space-between;
  width: 80%;
`;

const App = () => {
  const initialBank = [
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

  let [board, setBoard] = useState([]); // Now stores { tile, side }
  let [hand, setHand] = useState([]);
  let [bank, setBank] = useState(initialBank);
  let [leftEnd, setLeftEnd] = useState(-1);
  let [rightEnd, setRightEnd] = useState(-1);
  let [prediction, setPrediction] = useState('No prediction yet');
  let [history, setHistory] = useState([]);

  function getDirection(){
    
  }

  const handleDropOnBoard = (tile) => {

    console.log('App.js - handleDropOnBoard called with tile:', tile);
    if (board.some(t => t.tile.id === tile.id)){
      return;
    }
    let newTile = { tile }; // Create a new tile object
    console.log("Board size b4: ", board.length);
    
    if (board.length === 0) {
      board.push({ tile: newTile, side: 'center', flipped: false, position: null });
      leftEnd = tile.side1;
      rightEnd = tile.side2;
    } 
    else{

        const dropX = tile.dropX || 0; // You might need to pass dropX via the tile object
        const boardCenterX = 0; // This should be calculated based on the board's position
        const placeOnLeft = dropX < boardCenterX;
        
        if(tile.side1 === leftEnd && tile.side1 === rightEnd || tile.side2 === leftEnd && tile.side2 === rightEnd ){
          //CornerCase
          console.log("Corner case reached.");
          if(position.x < 0){
            if (tile.side1 === leftEnd) {
              newTile = { side1: tile.side2, side2: tile.side1, id: tile.id };
              board.unshift({ tile: newTile, side: 'left', flipped: true});
              leftEnd = tile.side2;
            } 
            else if (tile.side2 === leftEnd) {
              newTile = { side1: tile.side1, side2: tile.side2, id: tile.id };
              board.unshift({ tile: newTile, side: 'left', flipped: false });
              leftEnd = tile.side1;
            }
          }
          else if(position.x >= 0){
            if (tile.side1 === rightEnd) {
              newTile = { side1: tile.side1, side2: tile.side2, id: tile.id };
              board.push({ tile: newTile, side: 'right', flipped: false});
              rightEnd = tile.side2;
            } 
            else if (tile.side2 === rightEnd) {
              newTile = { side1: tile.side2, side2: tile.side1, id: tile.id };
              board.push({ tile: newTile, side: 'right', flipped: true});
              rightEnd = tile.side1;
            }
          }
          else{
            return; // Invalid move
          }
        }
        else{
          if (tile.side1 === leftEnd) {
            newTile = { side1: tile.side2, side2: tile.side1, id: tile.id };
            board.unshift({ tile: newTile, side: 'left', flipped: true});
            leftEnd = tile.side2;
          } 
          else if (tile.side2 === leftEnd) {
            newTile = { side1: tile.side1, side2: tile.side2, id: tile.id };
            board.unshift({ tile: newTile, side: 'left', flipped: false});
            leftEnd = tile.side1;
          }
          else if (tile.side1 === rightEnd) {
            newTile = { side1: tile.side1, side2: tile.side2, id: tile.id };
            board.push({ tile: newTile, side: 'right', flipped: false});
            rightEnd = tile.side2;
          } 
          else if (tile.side2 === rightEnd) {
            newTile = { side1: tile.side2, side2: tile.side1, id: tile.id };
            board.push({ tile: newTile, side: 'right', flipped: true});
            rightEnd = tile.side1;
          } 
          else{
            return; // Invalid move
          }
        }
    }

    hand = hand.filter(t => t.id !== tile.id);
    bank = bank.filter(t => t.id !== tile.id);

    setBoard(board);
    setBank(bank);
    setHand(hand);
    setLeftEnd(leftEnd);
    setRightEnd(rightEnd);
    setHistory([...history, { board: [...board], hand: [...hand], bank: [...bank], leftEnd, rightEnd }]);
    fetchPrediction(
      board.map(b => b.tile),
      hand,
      leftEnd,
      rightEnd
    );
  };

  function compareTiles(a, b){
    const aVal = a.side1 * 10 + a.side2;
    const bVal = b.side1 * 10 + b.side2;

    return aVal - bVal;
  }
  

  const handleDropOnBank = (tile) => {
    if (bank.some(t => t.id === tile.id)) return;

    if(tile.side1 > tile.side2){
      const a = tile.side1;
      tile.side1 = tile.side2;
      tile.side2 = a;
    }
    bank.unshift(tile);

    board = board.filter(t => t.tile.id !== tile.id);
    hand = hand.filter(t => t.id !== tile.id);
    setBoard(board);
    setHand(hand);
    bank.sort(compareTiles);
    setBank(bank);
    setHistory([...history, { board: [...board], hand: [...hand], bank: bank, leftEnd, rightEnd }]);
    fetchPrediction(
      board.map(b => b.tile),
      hand,
      leftEnd,
      rightEnd
    );
  };

  const handleDropOnHand = (tile) => {
    if (hand.some(t => t.id === tile.id)) return;
    //console.log(`handleDropOnHand: Dropping tile ${tile.id}`);
    //console.log('Current hand:', hand);
    hand.unshift(tile);
    hand.sort(compareTiles);
    board = board.filter(t => t.tile.id !== tile.id);
    bank = bank.filter(t => t.id !== tile.id);
    setBoard(board);
    setBank(bank);
    setHand(hand);
    setHistory([...history, { board: [...board], hand: [...hand], bank: [...bank], leftEnd, rightEnd }]);
    fetchPrediction(
      board.map(b => b.tile),
      hand,
      leftEnd,
      rightEnd
    );
  };

  const handleUndo = () => {
    if (history.length === 0) return;
    const lastState = history[history.length - 1];
    setBoard(lastState.board);
    setHand(lastState.hand);
    setBank(lastState.bank);
    setLeftEnd(lastState.leftEnd);
    setRightEnd(lastState.rightEnd);
    setHistory(history.slice(0, -1));
    fetchPrediction(
      lastState.board.map(b => b.tile),
      lastState.hand,
      lastState.leftEnd,
      lastState.rightEnd
    );
  };

  const handleAdjustPosition = (tileId, position) => {
    setBoard((prevBoard) =>
      prevBoard.map((t) =>
        t.tile.id === tileId ? { ...t, position } : t
      )
    );
  };

  const fetchPrediction = async (currentBoard, currentHand, currentLeftEnd, currentRightEnd) => {
    
    if(currentHand.length > 0){
      try {
        const response = await axios.post('http://localhost:8080/api/predict', {
          board: currentBoard,
          hand: currentHand,
          leftEnd: currentLeftEnd,
          rightEnd: currentRightEnd,
        });
        setPrediction(response.data);
      } catch (error) {
        setPrediction('Error fetching prediction');
        console.error(error);
      }
    }
  };

  return (
    <DndProvider backend={HTML5Backend}>
      <AppContainer >
        <h1>Dominoes Game</h1>
        <BoardContainer>
          <Board 
            tiles={board} 
            onDrop={handleDropOnBoard}
            leftEnd={leftEnd}
            rightEnd={rightEnd} 
            onAdjustPosition={handleAdjustPosition}
            />
          <MousePositionTracker ref = {board}/>
          <UndoButton onClick={handleUndo}>Undo</UndoButton>
        </BoardContainer>
        <BankContainer>
          <BankLabel>Bank</BankLabel>
          <Bank tiles={bank} onDrop={handleDropOnBank} />
        </BankContainer>
        <HandAndPrediction>
          <PlayerHand tiles={hand} onDrop={handleDropOnHand} />
          <ModelPrediction prediction={prediction} />
        </HandAndPrediction>
      </AppContainer>
    </DndProvider>
  );
};

export default App;
