from flask import Flask, request, jsonify
from flask_cors import CORS
import nltk
from nltk.tokenize import word_tokenize
from nltk.corpus import stopwords
from nltk.stem import WordNetLemmatizer
import string
import json

# Download required NLTK data
try:
    nltk.data.find('tokenizers/punkt')
except LookupError:
    nltk.download('punkt')

try:
    nltk.data.find('corpora/stopwords')
except LookupError:
    nltk.download('stopwords')

try:
    nltk.data.find('corpora/wordnet')
except LookupError:
    nltk.download('wordnet')

app = Flask(__name__)
CORS(app)

# Initialize NLTK components
lemmatizer = WordNetLemmatizer()
stop_words = set(stopwords.words('english'))

def preprocess_text(text):
    """Preprocess text using NLTK"""
    if not text:
        return []
    
    # Convert to lowercase
    text = text.lower()
    
    # Tokenize
    tokens = word_tokenize(text)
    
    # Remove punctuation and stopwords, and lemmatize
    processed_tokens = []
    for token in tokens:
        if token not in string.punctuation and token not in stop_words:
            lemma = lemmatizer.lemmatize(token)
            processed_tokens.append(lemma)
    
    return processed_tokens

def calculate_similarity(resume_skills, job_skills):
    """Calculate simple Jaccard similarity between skill sets"""
    if not resume_skills or not job_skills:
        return 0.0
    
    resume_set = set(resume_skills)
    job_set = set(job_skills)
    
    intersection = len(resume_set.intersection(job_set))
    union = len(resume_set.union(job_set))
    
    return intersection / union if union > 0 else 0.0

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({'status': 'AI Service is running!'})

@app.route('/match', methods=['POST'])
def match_jobs():
    try:
        data = request.get_json()
        
        resume_text = data.get('resume_text', '')
        job_description = data.get('job_description', '')
        
        # Preprocess both texts
        resume_skills = preprocess_text(resume_text)
        job_skills = preprocess_text(job_description)
        
        # Calculate similarity
        similarity_score = calculate_similarity(resume_skills, job_skills)
        
        # Return results
        return jsonify({
            'similarity_score': round(similarity_score, 2),
            'resume_skills_found': resume_skills[:20],  # Limit for display
            'job_skills_required': job_skills[:20],     # Limit for display
            'match_percentage': int(similarity_score * 100)
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/extract-skills', methods=['POST'])
def extract_skills():
    try:
        data = request.get_json()
        text = data.get('text', '')
        
        # Extract skills using simple keyword matching
        skills = preprocess_text(text)
        
        # Common technical skills to look for
        technical_skills = [
            'python', 'java', 'javascript', 'sql', 'html', 'css', 
            'react', 'angular', 'vue', 'node', 'express', 'django',
            'flask', 'spring', 'mongodb', 'mysql', 'postgresql',
            'aws', 'azure', 'docker', 'kubernetes', 'git', 'linux'
        ]
        
        # Filter for technical skills
        found_skills = [skill for skill in skills if skill in technical_skills]
        
        return jsonify({
            'extracted_skills': list(set(found_skills))  # Remove duplicates
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    print("Starting AI Service with NLTK...")
    app.run(host='0.0.0.0', port=5000, debug=True)
