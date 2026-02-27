// File: src/components/DreamRoleTestCreator.jsx
import React, { useState, useEffect } from 'react';
import { X, Plus, Trash2, BookOpen, Clock, Target, AlertCircle } from 'lucide-react';
import api from '../services/api';

const DreamRoleTestCreator = ({ recruiterId, onClose, onSuccess }) => {
  const [step, setStep] = useState(1); // 1: Basic Info, 2: Add Technical Questions
  const [branches, setBranches] = useState([]);
  const [branchStats, setBranchStats] = useState({});
  const [loading, setLoading] = useState(true);
  const [testId, setTestId] = useState(null);
  
  const [testInfo, setTestInfo] = useState({
    testName: '',
    description: '',
    durationMinutes: 60,
    passingScore: 60,
    targetBranch: ''
  });
  
  const [technicalQuestions, setTechnicalQuestions] = useState([]);
  const [currentQuestion, setCurrentQuestion] = useState({
    question: '',
    optionA: '',
    optionB: '',
    optionC: '',
    optionD: '',
    correctAnswer: 'A',
    explanation: '',
    difficultyLevel: 'Medium'
  });

  useEffect(() => {
    fetchBranchStats();
  }, []);

  const fetchBranchStats = async () => {
    try {
      setLoading(true);
      const response = await api.get('/dream-role-tests/branches/stats');
      setBranches(response.data.branches || []);
      setBranchStats(response.data.branchCounts || {});
    } catch (error) {
      console.error('Error fetching branch stats:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateTest = async () => {
    if (!testInfo.testName || !testInfo.targetBranch) {
      alert('Please fill all required fields');
      return;
    }

    try {
      const response = await api.post('/dream-role-tests/create', {
        recruiterId,
        ...testInfo
      });
      
      setTestId(response.data.test.id);
      setStep(2);
      alert('Test created successfully! Now add 10 technical questions.');
      
    } catch (error) {
      alert(error.response?.data?.error || 'Failed to create test');
    }
  };

  const handleAddQuestion = () => {
    if (!currentQuestion.question || !currentQuestion.optionA || !currentQuestion.optionB || 
        !currentQuestion.optionC || !currentQuestion.optionD || !currentQuestion.correctAnswer) {
      alert('Please fill all fields');
      return;
    }
    
    setTechnicalQuestions([...technicalQuestions, { ...currentQuestion, id: Date.now() }]);
    setCurrentQuestion({
      question: '',
      optionA: '',
      optionB: '',
      optionC: '',
      optionD: '',
      correctAnswer: 'A',
      explanation: '',
      difficultyLevel: 'Medium'
    });
  };

  const handleRemoveQuestion = (questionId) => {
    setTechnicalQuestions(technicalQuestions.filter(q => q.id !== questionId));
  };

  const handleSaveQuestions = async () => {
    if (technicalQuestions.length !== 10) {
      alert(`Please add exactly 10 technical questions. Current: ${technicalQuestions.length}`);
      return;
    }

    try {
      await api.post(`/dream-role-tests/${testId}/add-technical-questions`, technicalQuestions);
      alert('Technical questions added successfully!');
      if (onSuccess) onSuccess();
      if (onClose) onClose();
    } catch (error) {
      alert(error.response?.data?.error || 'Failed to save questions');
    }
  };

  if (loading) {
    return (
      <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
        <div className="relative top-20 mx-auto p-5 border w-full max-w-2xl shadow-lg rounded-md bg-white">
          <div className="text-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-2 text-gray-600">Loading branch statistics...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-10 mx-auto p-5 border w-full max-w-4xl shadow-lg rounded-md bg-white">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold text-gray-900">
            {step === 1 ? 'Create DreamRole Test' : 'Add Technical Questions'}
          </h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X className="h-6 w-6" />
          </button>
        </div>

        {/* Progress Steps */}
        <div className="flex justify-center mb-8">
          <div className="flex items-center">
            <div className={`flex items-center justify-center w-8 h-8 rounded-full ${
              step >= 1 ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-500'
            }`}>1</div>
            <div className={`ml-2 text-sm font-medium ${step >= 1 ? 'text-blue-600' : 'text-gray-500'}`}>
              Basic Info
            </div>
          </div>
          <div className="flex items-center mx-4">
            <div className="w-12 h-0.5 bg-gray-300"></div>
          </div>
          <div className="flex items-center">
            <div className={`flex items-center justify-center w-8 h-8 rounded-full ${
              step >= 2 ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-500'
            }`}>2</div>
            <div className={`ml-2 text-sm font-medium ${step >= 2 ? 'text-blue-600' : 'text-gray-500'}`}>
              Technical Questions (10)
            </div>
          </div>
        </div>

        {step === 1 && (
          <div className="space-y-6">
            {/* Branch Selection with Stats */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Select Branch <span className="text-red-500">*</span>
              </label>
              <select
                value={testInfo.targetBranch}
                onChange={(e) => setTestInfo({...testInfo, targetBranch: e.target.value})}
                className="w-full p-2 border border-gray-300 rounded-lg"
              >
                <option value="">Choose a branch</option>
                {branches.map(branch => (
                  <option key={branch} value={branch}>
                    {branch} ({branchStats[branch] || 0} questions available)
                  </option>
                ))}
              </select>
              {testInfo.targetBranch && branchStats[testInfo.targetBranch] < 40 && (
                <p className="mt-2 text-sm text-red-600 flex items-center">
                  <AlertCircle className="h-4 w-4 mr-1" />
                  Only {branchStats[testInfo.targetBranch]} questions available. Need at least 40.
                </p>
              )}
            </div>

            {/* Test Name */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Test Name <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={testInfo.testName}
                onChange={(e) => setTestInfo({...testInfo, testName: e.target.value})}
                className="w-full p-2 border border-gray-300 rounded-lg"
                placeholder="e.g., Software Developer DreamRole Test"
              />
            </div>

            {/* Description */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Description
              </label>
              <textarea
                value={testInfo.description}
                onChange={(e) => setTestInfo({...testInfo, description: e.target.value})}
                className="w-full p-2 border border-gray-300 rounded-lg"
                rows="3"
                placeholder="Brief description of the test"
              />
            </div>

            {/* Duration and Passing Score */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Duration (minutes) <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  value={testInfo.durationMinutes}
                  onChange={(e) => setTestInfo({...testInfo, durationMinutes: parseInt(e.target.value)})}
                  className="w-full p-2 border border-gray-300 rounded-lg"
                  min="30"
                  max="180"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Passing Score (%) <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  value={testInfo.passingScore}
                  onChange={(e) => setTestInfo({...testInfo, passingScore: parseInt(e.target.value)})}
                  className="w-full p-2 border border-gray-300 rounded-lg"
                  min="30"
                  max="90"
                />
              </div>
            </div>

            {/* Test Summary */}
            <div className="bg-blue-50 p-4 rounded-lg">
              <h3 className="font-semibold text-blue-800 mb-2">Test Configuration:</h3>
              <ul className="space-y-1 text-sm text-blue-700">
                <li>• 40 Aptitude questions (auto-selected from bank)</li>
                <li>• 10 Technical questions (you'll add manually)</li>
                <li>• Total: 50 questions</li>
                <li>• Questions are shuffled for each candidate</li>
              </ul>
            </div>

            <div className="flex justify-end space-x-3">
              <button
                onClick={onClose}
                className="px-4 py-2 border border-gray-300 rounded-lg text-sm"
              >
                Cancel
              </button>
              <button
                onClick={handleCreateTest}
                disabled={!testInfo.testName || !testInfo.targetBranch || branchStats[testInfo.targetBranch] < 40}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700 disabled:opacity-50"
              >
                Create Test & Continue
              </button>
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="space-y-6">
            <div className="bg-yellow-50 p-4 rounded-lg mb-4">
              <p className="text-sm text-yellow-800">
                Add 10 technical questions. Added: {technicalQuestions.length}/10
              </p>
            </div>

            <div className="grid grid-cols-2 gap-6">
              {/* Question Form */}
              <div className="bg-gray-50 p-4 rounded-lg">
                <h3 className="font-medium mb-3">Add Technical Question</h3>
                
                <div className="space-y-3">
                  <div>
                    <label className="block text-xs font-medium mb-1">Question *</label>
                    <textarea
                      value={currentQuestion.question}
                      onChange={(e) => setCurrentQuestion({...currentQuestion, question: e.target.value})}
                      className="w-full p-2 border border-gray-300 rounded-lg text-sm"
                      rows="2"
                      placeholder="Enter technical question"
                    />
                  </div>
                  
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-xs font-medium mb-1">Option A *</label>
                      <input
                        type="text"
                        value={currentQuestion.optionA}
                        onChange={(e) => setCurrentQuestion({...currentQuestion, optionA: e.target.value})}
                        className="w-full p-2 border border-gray-300 rounded-lg text-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium mb-1">Option B *</label>
                      <input
                        type="text"
                        value={currentQuestion.optionB}
                        onChange={(e) => setCurrentQuestion({...currentQuestion, optionB: e.target.value})}
                        className="w-full p-2 border border-gray-300 rounded-lg text-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium mb-1">Option C *</label>
                      <input
                        type="text"
                        value={currentQuestion.optionC}
                        onChange={(e) => setCurrentQuestion({...currentQuestion, optionC: e.target.value})}
                        className="w-full p-2 border border-gray-300 rounded-lg text-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium mb-1">Option D *</label>
                      <input
                        type="text"
                        value={currentQuestion.optionD}
                        onChange={(e) => setCurrentQuestion({...currentQuestion, optionD: e.target.value})}
                        className="w-full p-2 border border-gray-300 rounded-lg text-sm"
                      />
                    </div>
                  </div>
                  
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-xs font-medium mb-1">Correct Answer *</label>
                      <select
                        value={currentQuestion.correctAnswer}
                        onChange={(e) => setCurrentQuestion({...currentQuestion, correctAnswer: e.target.value})}
                        className="w-full p-2 border border-gray-300 rounded-lg text-sm"
                      >
                        <option value="A">A</option>
                        <option value="B">B</option>
                        <option value="C">C</option>
                        <option value="D">D</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-xs font-medium mb-1">Difficulty</label>
                      <select
                        value={currentQuestion.difficultyLevel}
                        onChange={(e) => setCurrentQuestion({...currentQuestion, difficultyLevel: e.target.value})}
                        className="w-full p-2 border border-gray-300 rounded-lg text-sm"
                      >
                        <option value="Easy">Easy</option>
                        <option value="Medium">Medium</option>
                        <option value="Hard">Hard</option>
                      </select>
                    </div>
                  </div>
                  
                  <div>
                    <label className="block text-xs font-medium mb-1">Explanation (Optional)</label>
                    <textarea
                      value={currentQuestion.explanation}
                      onChange={(e) => setCurrentQuestion({...currentQuestion, explanation: e.target.value})}
                      className="w-full p-2 border border-gray-300 rounded-lg text-sm"
                      rows="2"
                      placeholder="Explain the correct answer"
                    />
                  </div>
                  
                  <button
                    onClick={handleAddQuestion}
                    className="w-full bg-blue-600 text-white py-2 rounded-lg text-sm font-medium hover:bg-blue-700"
                  >
                    <Plus className="h-4 w-4 inline mr-1" />
                    Add Question
                  </button>
                </div>
              </div>

              {/* Questions List */}
              <div>
                <h3 className="font-medium mb-3">Added Questions ({technicalQuestions.length}/10)</h3>
                <div className="space-y-2 max-h-96 overflow-y-auto">
                  {technicalQuestions.map((q, index) => (
                    <div key={q.id} className="bg-white border border-gray-200 rounded-lg p-3 text-sm relative group">
                      <div className="flex justify-between items-start">
                        <span className="font-medium">Q{index + 1}: {q.question.substring(0, 50)}...</span>
                        <button
                          onClick={() => handleRemoveQuestion(q.id)}
                          className="text-red-500 hover:text-red-700"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                      <div className="flex items-center space-x-2 mt-1">
                        <span className={`px-2 py-0.5 rounded text-xs ${
                          q.difficultyLevel === 'Easy' ? 'bg-green-100 text-green-800' :
                          q.difficultyLevel === 'Medium' ? 'bg-yellow-100 text-yellow-800' :
                          'bg-red-100 text-red-800'
                        }`}>
                          {q.difficultyLevel}
                        </span>
                        <span className="text-xs text-gray-500">Correct: {q.correctAnswer}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div className="flex justify-end space-x-3 mt-6">
              <button
                onClick={onClose}
                className="px-4 py-2 border border-gray-300 rounded-lg text-sm"
              >
                Cancel
              </button>
              <button
                onClick={handleSaveQuestions}
                disabled={technicalQuestions.length !== 10}
                className="px-4 py-2 bg-green-600 text-white rounded-lg text-sm hover:bg-green-700 disabled:opacity-50"
              >
                Save & Complete Test
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default DreamRoleTestCreator;