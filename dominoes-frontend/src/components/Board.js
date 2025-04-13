import React, { useRef, useEffect, useState, useCallback } from 'react';
import { useDrop } from 'react-dnd';
import styled from 'styled-components';
import Tile, { ItemTypes } from './Tile';

const BoardArea = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-color: #f0f0f0;
  position: relative;
  &[data-is-over='true'] {
    background-color: #e0f7fa;
  }
`;

const TileWrapper = styled.div`
  position: absolute;
  left: ${({ x }) => x}px;
  top: ${({ y }) => y}px;
`;

const Board = ({ tiles, onDrop, leftEnd, rightEnd, onAdjustPosition }) => {
  const boardRef = useRef(null);
  const [boardRect, setBoardRect] = useState(null);
  const [lastDroppedTile, setLastDroppedTile] = useState(null);
  const [dropPosition, setDropPosition] = useState(null);
  const [finalPosition, setFinalPosition] = useState(null);
  const [isBoardReady, setIsBoardReady] = useState(false);

  const updateBoardRect = () => {
    if (boardRef.current) {
      const rect = boardRef.current.getBoundingClientRect();
      console.log('Board.js - BoardArea dimensions:', {
        width: rect.width,
        height: rect.height,
        display: window.getComputedStyle(boardRef.current).display,
        visibility: window.getComputedStyle(boardRef.current).visibility,
      });
      if (rect.width === 0 || rect.height === 0) {
        //console.log('Board.js - BoardArea has zero dimensions, retrying...');
        setTimeout(updateBoardRect, 100);
        return;
      }
      setBoardRect({
        left: rect.left,
        top: rect.top,
        right: rect.right,
        bottom: rect.bottom,
        width: rect.width,
        height: rect.height,
      });
      setIsBoardReady(true);
      console.log('Board.js - BoardRect set:', {
        left: rect.left,
        top: rect.top,
        right: rect.right,
        bottom: rect.bottom,
        width: rect.width,
        height: rect.height,
      });
    } else {
      //console.log('Board.js - boardRef.current is null, retrying...');
      setTimeout(updateBoardRect, 100);
    }
  };

  useEffect(() => {
    updateBoardRect();
    window.addEventListener('resize', updateBoardRect);
    window.addEventListener('scroll', updateBoardRect);
    return () => {
      window.removeEventListener('resize', updateBoardRect);
      window.removeEventListener('scroll', updateBoardRect);
    };
  }, []);

  const calculateTileWidth = (tile) => {
    const isVertical = tile.side1 === tile.side2 || tile.orientation === 'vertical';
    return isVertical ? 52 : 92;
  };

  const calculateChainPositions = (tilesToPosition) => {
    if (!boardRect) {
      console.log('Board.js - boardRect is null in calculateChainPositions');
      return tilesToPosition;
    }

    const tileWidths = tilesToPosition.map((t) => calculateTileWidth(t.tile));
    const totalWidth = tileWidths.reduce((sum, width) => sum + width, 0) + (tilesToPosition.length - 1) * 5;

    const chainStartX = boardRect.left + boardRect.width / 2 - totalWidth / 2;
    const chainY = boardRect.top + boardRect.height / 2;

    let currentX = chainStartX;
    return tilesToPosition.map((t, index) => {
      const tileX = currentX;
      const tileY = chainY;
      currentX += tileWidths[index] + 5;
      return {
        ...t,
        position: { x: tileX, y: tileY },
      };
    });
  };

  useEffect(() => {
    if (tiles.length === 0 || !boardRect) return;

    console.log('Board.js - Recalculating chain positions for tiles:', tiles);
    const updatedTiles = calculateChainPositions(tiles);
    updatedTiles.forEach((t) => {
      console.log(`Board.js - Updating position for tile ${t.tile.id}:`, t.position);
      onAdjustPosition(t.tile.id, t.position);
    });
  }, [tiles, boardRect, onAdjustPosition]);

  const calculateFinalPosition = (newTiles, droppedTileId) => {
    if (!boardRect) {
      console.log('Board.js - boardRect is null in calculateFinalPosition');
      return null;
    }

    const tileWidths = newTiles.map((t) => calculateTileWidth(t.tile));
    const totalWidth = tileWidths.reduce((sum, width) => sum + width, 0) + (newTiles.length - 1) * 5;

    const droppedTileIndex = newTiles.findIndex((t) => t.tile.id === droppedTileId);
    if (droppedTileIndex === -1) return null;

    let offsetX = 0;
    for (let i = 0; i < droppedTileIndex; i++) {
      offsetX += tileWidths[i] + 5;
    }

    const chainStartX = boardRect.left + boardRect.width / 2 - totalWidth / 2;
    const tileX = chainStartX + offsetX;
    const tileY = boardRect.top + boardRect.height / 2;

    return { x: tileX, y: tileY };
  };

  const adjustPosition = (position, tile) => {
    if (!position || !boardRect) {
      console.log('Board.js - position or boardRect is null in adjustPosition');
      return position;
    }

    const tileX = position.x;
    const tileY = position.y;

    const distanceToLeft = Math.abs(tileX - boardRect.left);
    const distanceToRight = Math.abs(tileX - boardRect.right);
    const distanceToTop = Math.abs(tileY - boardRect.top);
    const distanceToBottom = Math.abs(tileY - boardRect.bottom);

    const distances = [
      { wall: 'left', distance: distanceToLeft },
      { wall: 'right', distance: distanceToRight },
      { wall: 'top', distance: distanceToTop },
      { wall: 'bottom', distance: distanceToBottom },
    ];

    const closest = distances.reduce((min, current) =>
      current.distance < min.distance ? current : min,
      distances[0]
    );

    const minDistance = 20;
    let adjustedX = tileX;
    let adjustedY = tileY;

    if (closest.wall === 'left' && closest.distance < minDistance) {
      adjustedX = boardRect.left + minDistance;
    } else if (closest.wall === 'right' && closest.distance < minDistance) {
      adjustedX = boardRect.right - minDistance;
    }

    if (closest.wall === 'top' && closest.distance < minDistance) {
      adjustedY = boardRect.top + minDistance;
    } else if (closest.wall === 'bottom' && closest.distance < minDistance) {
      adjustedY = boardRect.bottom - minDistance;
    }

    const gridSize = 50;
    adjustedX = Math.round(adjustedX / gridSize) * gridSize;
    adjustedY = Math.round(adjustedY / gridSize) * gridSize;

    const adjustedPosition = { x: adjustedX, y: adjustedY };
    console.log(`Board.js - Adjusted position for tile ${tile.id}:`, adjustedPosition);

    return adjustedPosition;
  };

  const [, drop] = useDrop(() => ({
    accept: ItemTypes.TILE,
    drop: (item, monitor) => {
      console.log('Board.js - Tile dropped:', item);
      const clientOffset = monitor.getClientOffset();
      console.log('Board.js - ClientOffset:', clientOffset);

      if (!clientOffset) {
        console.log('Board.js - ClientOffset is null');
        return;
      }

      if (!boardRect) {
        console.log('Board.js - boardRect is null, deferring drop');
        setTimeout(() => {
          if (boardRect) {
            handleDrop(item, clientOffset);
          } else {
            //console.log('Board.js - boardRect still null after deferring, dropping without position calculation');
            onDrop(item);
          }
        }, 100);
        return;
      }

      handleDrop(item, clientOffset);
    },
  }));

  const handleDrop = (item, clientOffset) => {
    let newTiles = [...tiles];
    let newTile = { tile: { ...item }, flipped: false };

    if (tiles.length === 0) {
      newTiles.push({ tile: newTile.tile, side: 'center', flipped: false });
    } else {
      const dropX = clientOffset.x;
      const boardCenterX = boardRect.left + boardRect.width / 2;
      const placeOnLeft = dropX < boardCenterX;

      if (placeOnLeft) {
        if (item.side1 === leftEnd) {
          newTile = { tile: { side1: item.side2, side2: item.side1, id: item.id }, flipped: true };
          newTiles.unshift({ tile: newTile.tile, side: 'left', flipped: true });
        } else if (item.side2 === leftEnd) {
          newTiles.unshift({ tile: newTile.tile, side: 'left', flipped: false });
        }
      } else {
        if (item.side1 === rightEnd) {
          newTiles.push({ tile: newTile.tile, side: 'right', flipped: false });
        } else if (item.side2 === rightEnd) {
          newTile = { tile: { side1: item.side2, side2: item.side1, id: item.id }, flipped: true };
          newTiles.push({ tile: newTile.tile, side: 'right', flipped: true });
        }
      }
    }

    let finalPos = calculateFinalPosition(newTiles, item.id);
    console.log('Board.js - Final position of dropped tile:', finalPos);

    finalPos = adjustPosition(finalPos, item);
    setFinalPosition(finalPos);

    if (onAdjustPosition && finalPos) {
      onAdjustPosition(item.id, finalPos);
    }

    onDrop(item);

    setDropPosition({
      x: clientOffset.x,
      y: clientOffset.y,
    });
    setLastDroppedTile(item.id);
    console.log('Board.js - DropPosition set:', { x: clientOffset.x, y: clientOffset.y });
    console.log('Board.js - LastDroppedTile set:', item.id);
  };

  // Combine the boardRef and drop ref
  const setRef = useCallback((node) => {
    // Set the boardRef
    boardRef.current = node;
    // Call the drop ref function
    drop(node);
  }, [drop]);

  useEffect(() => {
    tiles.forEach((t) => {
      //console.log(`Board.js - Rendering tile ${t.tile.id}, dropPosition:`, t.tile.id === lastDroppedTile ? dropPosition : null);
    });
  }, [tiles, lastDroppedTile, dropPosition]);

  return (
    <BoardArea ref={setRef}>
      {isBoardReady ? (
        tiles.map((t) => {
          const x = t.position ? t.position.x - (boardRect?.left || 0) : (boardRect?.width || 0) / 2;
          const y = t.position ? t.position.y - (boardRect?.top || 0) : (boardRect?.height || 0) / 2;
          console.log(`Board.js - Rendering tile ${t.tile.id} at position: (${x}, ${y})`);
          return (
            <TileWrapper key={t.tile.id} x={x} y={y}>
              <Tile
                side1={t.tile.side1}
                side2={t.tile.side2}
                id={t.tile.id}
                flipped={t.flipped}
                orientation={t.orientation}
                boardRect={boardRect}
                dropPosition={t.tile.id === lastDroppedTile ? dropPosition : null}
                finalPosition={t.tile.id === lastDroppedTile ? finalPosition : null}
              />
            </TileWrapper>
          );
        })
      ) : (
        <div>Loading board...</div>
      )}
    </BoardArea>
  );
};

export default Board;