from flask import Flask, request, jsonify
from flask_cors import CORS
import nltk
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize
import spacy
import re
import json
import os

# Download required NLTK data
nltk.download('punkt', quiet=True)
nltk.download('stopwords', quiet=True)
nltk.download('averaged_perceptron_tagger', quiet=True)

app = Flask(__name__)
CORS(app)

# Load spaCy model
try:
    nlp = spacy.load("en_core_web_sm")
except OSError:
    print("Downloading spaCy model...")
    from spacy.cli import download
    download("en_core_web_sm")
    nlp = spacy.load("en_core_web_sm")

# Technical skills database
TECHNICAL_SKILLS = {
    'programming_languages': [
        'java', 'python', 'javascript', 'typescript', 'c++', 'c#', 'ruby', 'php',
        'swift', 'kotlin', 'go', 'rust', 'scala'
    ],
    'frameworks': [
        'spring boot', 'spring', 'django', 'flask', 'react', 'angular', 'vue',
        'node.js', 'express.js', 'laravel', 'ruby on rails'
    ],
    'databases': [
        'mysql', 'postgresql', 'mongodb', 'redis', 'oracle', 'sql server',
        'cassandra', 'elasticsearch'
    ],
    'cloud_platforms': [
        'aws', 'azure', 'google cloud', 'docker', 'kubernetes', 'jenkins',
        'terraform', 'ansible'
    ],
    'tools': [
        'git', 'jira', 'confluence', 'slack', 'maven', 'gradle'
    ]
}

@app.route('/api/analyze/resume', methods=['POST'])
def analyze_resume():
    try:
        data = request.get_json()
        text = data.get('text', '')
        
        if not text:
            return jsonify({'error': 'No text provided'}), 400
        
        # Extract skills
        skills = extract_skills(text)
        
        # Extract experience
        experience = extract_experience(text)
        
        # Extract education
        education = extract_education(text)
        
        # Calculate match score for different job categories
        job_recommendations = recommend_jobs(skills, experience)
        
        return jsonify({
            'skills': skills,
            'experience_years': experience,
            'education': education,
            'job_recommendations': job_recommendations
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/health', methods=['GET'])
def health_check():
    return jsonify({'status': 'AI Service is running'})

def extract_skills(text):
    text_lower = text.lower()
    found_skills = set()
    
    # Check for technical skills
    for category, skills in TECHNICAL_SKILLS.items():
        for skill in skills:
            if skill in text_lower:
                found_skills.add(skill)
    
    # Use spaCy for more advanced skill extraction
    doc = nlp(text)
    
    # Extract nouns and proper nouns that might be skills
    for token in doc:
        if (token.pos_ in ['NOUN', 'PROPN'] and 
            token.text.lower() not in stopwords.words('english') and
            len(token.text) > 2):
            found_skills.add(token.text.lower())
    
    return list(found_skills)

def extract_experience(text):
    # Look for experience patterns
    experience_patterns = [
        r'(\d+)\s*\+?\s*years?\s*(?:of)?\s*experience',
        r'experience\s*:\s*(\d+)\s*years?',
        r'(\d+)\s*-\s*(\d+)\s*years?\s*experience'
    ]
    
    for pattern in experience_patterns:
        matches = re.findall(pattern, text, re.IGNORECASE)
        if matches:
            if isinstance(matches[0], tuple):
                return int(matches[0][1])  # Take the higher number in range
            else:
                return int(matches[0])
    
    # Fallback: look for dates that might indicate work duration
    year_matches = re.findall(r'(19|20)\d{2}', text)
    if year_matches:
        years = [int(year) for year in year_matches if 1900 <= int(year) <= 2030]
        if len(years) >= 2:
            return max(years) - min(years)
    
    return 0

def extract_education(text):
    education_keywords = {
        'phd': ['phd', 'ph.d', 'doctorate'],
        'masters': ['master', 'm.s', 'm.sc', 'm.tech', 'm.e', 'm.a'],
        'bachelors': ['bachelor', 'b.s', 'b.sc', 'b.tech', 'b.e', 'b.a'],
        'associate': ['associate', 'diploma']
    }
    
    text_lower = text.lower()
    
    for level, keywords in education_keywords.items():
        for keyword in keywords:
            if keyword in text_lower:
                return level.capitalize()
    
    return 'Not specified'

def recommend_jobs(skills, experience):
    job_categories = {
        'backend_developer': {
            'required': ['java', 'python', 'spring boot', 'mysql', 'rest api'],
            'weight': 1.0
        },
        'frontend_developer': {
            'required': ['javascript', 'react', 'html', 'css', 'typescript'],
            'weight': 1.0
        },
        'fullstack_developer': {
            'required': ['javascript', 'react', 'node.js', 'mysql', 'rest api'],
            'weight': 1.0
        },
        'devops_engineer': {
            'required': ['aws', 'docker', 'kubernetes', 'linux', 'jenkins'],
            'weight': 1.0
        },
        'data_scientist': {
            'required': ['python', 'machine learning', 'sql', 'statistics'],
            'weight': 1.0
        }
    }
    
    recommendations = []
    
    for category, details in job_categories.items():
        matched_skills = set(skills) & set(details['required'])
        match_score = len(matched_skills) / len(details['required'])
        
        # Adjust score based on experience
        experience_bonus = min(experience / 10, 0.3)  # Max 30% bonus for experience
        
        final_score = (match_score * 0.7) + (experience_bonus * 0.3)
        
        recommendations.append({
            'category': category.replace('_', ' ').title(),
            'match_score': round(final_score * 100, 2),
            'matched_skills': list(matched_skills),
            'missing_skills': list(set(details['required']) - set(skills))
        })
    
    # Sort by match score descending
    recommendations.sort(key=lambda x: x['match_score'], reverse=True)
    
    return recommendations

if __name__ == '__main__':
    print("Starting AI Service on port 5000...")
    app.run(host='0.0.0.0', port=5000, debug=True)