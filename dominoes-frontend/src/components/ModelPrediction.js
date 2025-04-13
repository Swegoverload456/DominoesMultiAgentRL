import React from 'react';
import styled from 'styled-components';

const PredictionContainer = styled.div`
  border: 1px solid black;
  padding: 10px;
  width: 200px;
  text-align: center;
`;

const PredictionLabel = styled.div`
  font-weight: bold;
  margin-bottom: 10px;
`;

const ModelPrediction = ({ prediction }) => (
  <PredictionContainer>
    <PredictionLabel>You Should Play:</PredictionLabel>
    <div>{prediction}</div>
  </PredictionContainer>
);

export default ModelPrediction;