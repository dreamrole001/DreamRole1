from flask import Flask, request, jsonify
from flask_cors import CORS
import nltk
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize
import re
import json
import os

# Download required NLTK data
nltk.download('punkt', quiet=True)
nltk.download('stopwords', quiet=True)
nltk.download('averaged_perceptron_tagger', quiet=True)

app = Flask(__name__)
CORS(app)

# ENHANCED Technical skills database with comprehensive skills
TECHNICAL_SKILLS = {
    'programming_languages': [
        'java', 'python', 'javascript', 'typescript', 'c++', 'c#', 'ruby', 'php',
        'swift', 'kotlin', 'go', 'rust', 'scala', 'r', 'matlab', 'perl', 'dart',
        'html', 'css', 'sql', 'bash', 'shell', 'powershell'
    ],
    'frontend_development': [
        'react', 'angular', 'vue', 'html', 'css', 'sass', 'less', 'bootstrap', 
        'tailwind', 'webpack', 'vite', 'next.js', 'nuxt.js', 'jquery', 'redux',
        'vuex', 'material-ui', 'ant design', 'chakra ui'
    ],
    'backend_development': [
        'node.js', 'spring boot', 'spring', 'django', 'flask', 'express.js', 
        'laravel', 'ruby on rails', 'asp.net', 'fastapi', 'graphql', 'rest api',
        'microservices', 'serverless', 'spring mvc', 'spring security'
    ],
    'databases': [
        'mysql', 'postgresql', 'mongodb', 'redis', 'oracle', 'sql server',
        'cassandra', 'elasticsearch', 'dynamodb', 'firebase', 'sqlite', 'mariadb',
        'cosmos db', 'couchbase', 'neo4j', 'hbase'
    ],
    'cloud_platforms': [
        'aws', 'azure', 'google cloud', 'docker', 'kubernetes', 'jenkins',
        'terraform', 'ansible', 'ci/cd', 'github actions', 'gitlab ci',
        'aws ec2', 'aws s3', 'aws lambda', 'azure devops', 'gcp'
    ],
    'mobile_development': [
        'android', 'ios', 'react native', 'flutter', 'xamarin', 'swiftui', 'jetpack compose',
        'kotlin android', 'java android', 'objective-c', 'swift'
    ],
    'ai_ml_data_science': [
        'machine learning', 'deep learning', 'tensorflow', 'pytorch', 'scikit-learn',
        'nlp', 'computer vision', 'data science', 'pandas', 'numpy', 'opencv',
        'keras', 'data analysis', 'big data', 'hadoop', 'spark', 'tableau', 'power bi'
    ],
    'devops_tools': [
        'git', 'jira', 'confluence', 'slack', 'maven', 'gradle', 'npm', 'yarn',
        'postman', 'swagger', 'figma', 'jenkins', 'sonarqube', 'prometheus', 'grafana',
        'splunk', 'new relic', 'datadog'
    ],
    'engineering': [
        'autocad', 'solidworks', 'catia', 'ansys', 'finite element analysis', 'fea',
        'computational fluid dynamics', 'cfd', 'matlab', 'product design', 'cad',
        'revit', 'staad pro', 'etabs', 'structural analysis', 'construction management',
        'circuit design', 'power systems', 'control systems', 'embedded systems',
        'plc programming', 'scada', 'arduino', 'raspberry pi', 'iot', 'vlsi'
    ],
    'business_management': [
        'project management', 'agile', 'scrum', 'kanban', 'jira', 'confluence',
        'strategic planning', 'business development', 'market research',
        'financial analysis', 'budgeting', 'forecasting', 'stakeholder management',
        'risk management', 'change management', 'process improvement', 'six sigma'
    ],
    'sales_marketing': [
        'digital marketing', 'seo', 'sem', 'social media marketing', 'content marketing',
        'email marketing', 'market research', 'sales strategy', 'customer relationship management',
        'crm', 'salesforce', 'hubspot', 'google analytics', 'brand management'
    ],
    'healthcare_medical': [
        'patient care', 'medical terminology', 'electronic health records', 'ehr',
        'healthcare management', 'clinical research', 'pharmacy', 'nursing',
        'medical coding', 'icd-10', 'cpr', 'first aid', 'health informatics'
    ],
    'design_creative': [
        'ui design', 'ux design', 'user research', 'wireframing', 'prototyping',
        'adobe photoshop', 'adobe illustrator', 'adobe indesign', 'figma',
        'sketch', 'adobe xd', 'graphic design', 'web design', 'motion graphics'
    ]
}

# Skill importance weights
SKILL_IMPORTANCE_WEIGHTS = {
    'programming_languages': 1.3,
    'backend_development': 1.2,
    'frontend_development': 1.2,
    'ai_ml_data_science': 1.4,
    'cloud_platforms': 1.1,
    'databases': 1.1,
    'engineering': 1.2,
    'business_management': 1.1,
    'sales_marketing': 1.0,
    'healthcare_medical': 1.1,
    'design_creative': 1.0,
    'mobile_development': 1.1,
    'devops_tools': 0.9
}

# ROOT ROUTE - FIXES "Not Found" ERROR
@app.route('/')
def home():
    return jsonify({
        'message': 'AI Service is running!',
        'endpoints': {
            'health': '/api/health',
            'analyze_resume': '/api/analyze/resume',
            'match_jobs': '/match',
            'extract_skills': '/extract-skills'
        }
    })

# SIMPLE HEALTH CHECK
@app.route('/health', methods=['GET'])
def health_simple():
    return jsonify({'status': 'AI Service is running!'})

# ENHANCED JOB MATCHING ENDPOINT
@app.route('/match', methods=['POST'])
def match_jobs():
    try:
        data = request.get_json()
        
        resume_text = data.get('resume_text', '')
        job_description = data.get('job_description', '')
        
        if not resume_text or not job_description:
            return jsonify({'error': 'Both resume_text and job_description are required'}), 400

        # Extract skills from both texts with enhanced matching
        resume_skills = extract_skills_enhanced(resume_text)
        job_skills = extract_skills_enhanced(job_description)
        
        # Calculate enhanced similarity with weights
        similarity_score = calculate_enhanced_similarity(resume_skills, job_skills)
        
        # Get matched and missing skills
        matched_skills = find_matched_skills(resume_skills, job_skills)
        missing_skills = find_missing_skills(resume_skills, job_skills)
        
        return jsonify({
            'similarity_score': round(similarity_score, 2),
            'match_percentage': int(similarity_score * 100),
            'resume_skills_found': resume_skills[:25],
            'job_skills_required': job_skills[:25],
            'matched_skills': matched_skills,
            'missing_skills': missing_skills,
            'analysis': {
                'total_resume_skills': len(resume_skills),
                'total_job_skills': len(job_skills),
                'skills_matched': len(matched_skills),
                'skills_missing': len(missing_skills)
            }
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ENHANCED SKILL EXTRACTION ENDPOINT
@app.route('/extract-skills', methods=['POST'])
def extract_skills_endpoint():
    try:
        data = request.get_json()
        text = data.get('text', '')
        
        if not text:
            return jsonify({'error': 'No text provided'}), 400
            
        skills = extract_skills_enhanced(text)
        
        return jsonify({
            'extracted_skills': skills,
            'total_skills': len(skills),
            'skill_categories': categorize_skills(skills)
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ENHANCED RESUME ANALYSIS ENDPOINT
@app.route('/api/analyze/resume', methods=['POST'])
def analyze_resume():
    try:
        data = request.get_json()
        text = data.get('text', '')

        if not text:
            return jsonify({'error': 'No text provided'}), 400

        # Extract skills with enhanced method
        skills = extract_skills_enhanced(text)

        # Extract experience with enhanced patterns
        experience = extract_experience_enhanced(text)

        # Extract education with enhanced patterns
        education = extract_education_enhanced(text)

        # Calculate enhanced job recommendations
        job_recommendations = recommend_jobs_enhanced(skills, experience, education)

        # Get skill categories
        skill_categories = categorize_skills(skills)

        return jsonify({
            'skills': skills,
            'skill_categories': skill_categories,
            'experience_years': experience,
            'education': education,
            'job_recommendations': job_recommendations,
            'analysis_summary': {
                'total_skills': len(skills),
                'primary_domain': get_primary_domain(skill_categories),
                'experience_level': get_experience_level(experience),
                'skill_diversity': len(skill_categories)
            }
        })

    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ORIGINAL HEALTH CHECK
@app.route('/api/health', methods=['GET'])
def health_check():
    return jsonify({'status': 'AI Service is running'})

# ENHANCED SKILL EXTRACTION FUNCTION
def extract_skills_enhanced(text):
    text_lower = text.lower()
    found_skills = set()

    # Check for technical skills with word boundaries
    for category, skills in TECHNICAL_SKILLS.items():
        for skill in skills:
            # Use word boundaries for better matching
            pattern = r'\b' + re.escape(skill) + r'\b'
            if re.search(pattern, text_lower):
                found_skills.add(skill)

    # Use NLTK for advanced noun extraction
    tokens = word_tokenize(text_lower)
    pos_tags = nltk.pos_tag(tokens)
    
    # Extract nouns and proper nouns that might be skills
    for token, pos in pos_tags:
        if (pos.startswith('NN') and 
            token not in stopwords.words('english') and 
            len(token) > 2 and
            any(char.isalpha() for char in token)):
            found_skills.add(token)

    return list(found_skills)

# ENHANCED EXPERIENCE EXTRACTION FUNCTION
def extract_experience_enhanced(text):
    # Enhanced experience patterns
    experience_patterns = [
        r'(\d+)\s*\+?\s*years?\s*(?:of)?\s*(?:experience|exp)',
        r'experience\s*[:\-]?\s*(\d+)\s*years?',
        r'(\d+)\s*-\s*(\d+)\s*years?\s*experience',
        r'(\d+)\s*yr',
        r'(\d+)\s*years',
        r'(\d+)\+?\s*(?:years?|yrs?)(?:\s+of)?\s*(?:work|professional)'
    ]

    for pattern in experience_patterns:
        matches = re.findall(pattern, text, re.IGNORECASE)
        if matches:
            if isinstance(matches[0], tuple):
                # For range patterns like "3-5 years"
                try:
                    years = int(matches[0][1])
                    return min(years, 30)  # Cap at 30 years
                except:
                    continue
            else:
                try:
                    years = int(matches[0])
                    return min(years, 30)  # Cap at 30 years
                except:
                    continue

    # Fallback: estimate from work history
    work_pattern = r'(?:19|20)\d{2}\s*[-–]\s*(?:present|current|now|(?:19|20)\d{2})'
    work_periods = re.findall(work_pattern, text, re.IGNORECASE)
    if work_periods:
        return min(len(work_periods) * 2, 20)  # Estimate 2 years per position

    return 0

# ENHANCED EDUCATION EXTRACTION FUNCTION
def extract_education_enhanced(text):
    education_keywords = {
        'PhD': ['phd', 'ph.d', 'doctorate', 'doctor of'],
        'Master\'s': ['master', 'm.s', 'm.sc', 'm.tech', 'm.e', 'm.a', 'ms', 'masters', 'post graduate'],
        'Bachelor\'s': ['bachelor', 'b.s', 'b.sc', 'b.tech', 'b.e', 'b.a', 'bs', 'be', 'bachelors', 'b\\.e\\.', 'b\\.tech', 'graduat'],
        'Associate/Diploma': ['associate', 'diploma', 'associate degree'],
        'High School': ['high school', 'secondary', 'higher secondary', 'ssc', 'hsc', 'intermediate']
    }

    text_lower = text.lower()

    for level, keywords in education_keywords.items():
        for keyword in keywords:
            pattern = r'\b' + re.escape(keyword) + r'\b'
            if re.search(pattern, text_lower):
                return level

    return 'Not specified'

# ENHANCED JOB RECOMMENDATION FUNCTION
def recommend_jobs_enhanced(skills, experience, education):
    job_categories = {
        'Full Stack Developer': {
            'required': ['javascript', 'html', 'css', 'react', 'node.js', 'mongodb', 'git'],
            'domain': 'IT & Software',
            'experience_range': (1, 8),
            'education_weight': 0.8
        },
        'Frontend Developer': {
            'required': ['javascript', 'html', 'css', 'react', 'typescript', 'webpack'],
            'domain': 'IT & Software',
            'experience_range': (1, 6),
            'education_weight': 0.7
        },
        'Backend Developer': {
            'required': ['java', 'python', 'node.js', 'mysql', 'mongodb', 'rest api'],
            'domain': 'IT & Software', 
            'experience_range': (2, 10),
            'education_weight': 0.8
        },
        'DevOps Engineer': {
            'required': ['docker', 'kubernetes', 'aws', 'jenkins', 'terraform', 'linux'],
            'domain': 'IT & Software',
            'experience_range': (3, 12),
            'education_weight': 0.9
        },
        'Data Scientist': {
            'required': ['python', 'machine learning', 'pandas', 'numpy', 'sql', 'statistics'],
            'domain': 'IT & Software',
            'experience_range': (2, 8),
            'education_weight': 1.0
        },
        'Mechanical Engineer': {
            'required': ['autocad', 'solidworks', 'fea', 'matlab', 'mechanical design'],
            'domain': 'Engineering',
            'experience_range': (1, 10),
            'education_weight': 0.9
        },
        'Project Manager': {
            'required': ['project management', 'agile', 'scrum', 'jira', 'leadership'],
            'domain': 'Business & Management',
            'experience_range': (3, 15),
            'education_weight': 0.8
        },
        'Business Analyst': {
            'required': ['business analysis', 'requirements gathering', 'sql', 'excel'],
            'domain': 'Business & Management',
            'experience_range': (2, 8),
            'education_weight': 0.8
        }
    }

    recommendations = []

    for category, details in job_categories.items():
        # Calculate skill match
        matched_skills = set(skills) & set(details['required'])
        skill_match_score = len(matched_skills) / len(details['required'])
        
        # Calculate experience match
        exp_min, exp_max = details['experience_range']
        if experience >= exp_min and experience <= exp_max:
            experience_score = 1.0
        elif experience < exp_min:
            experience_score = max(0.3, experience / exp_min)
        else:
            experience_score = max(0.6, 1.0 - (experience - exp_max) / 10)
        
        # Calculate education bonus
        education_bonus = 0.0
        if education in ['Bachelor\'s', 'Master\'s', 'PhD']:
            education_bonus = details['education_weight'] * 0.2
        
        # Final score calculation
        final_score = (skill_match_score * 0.6) + (experience_score * 0.3) + (education_bonus * 0.1)
        
        # Apply domain bonus if skills match the domain
        domain_bonus = 0.0
        user_domains = [cat for cat in categorize_skills(skills).keys()]
        if details['domain'] in user_domains:
            domain_bonus = 0.1
        
        final_score = min(final_score + domain_bonus, 1.0)

        recommendations.append({
            'category': category,
            'match_score': round(final_score * 100, 2),
            'matched_skills': list(matched_skills),
            'missing_skills': list(set(details['required']) - set(skills)),
            'domain': details['domain'],
            'experience_suitability': get_experience_suitability(experience, exp_min, exp_max)
        })

    # Sort by match score descending
    recommendations.sort(key=lambda x: x['match_score'], reverse=True)

    return recommendations[:8]  # Return top 8 recommendations

# ENHANCED SIMILARITY CALCULATION FUNCTION
def calculate_enhanced_similarity(resume_skills, job_skills):
    if not resume_skills or not job_skills:
        return 0.0
    
    resume_set = set(resume_skills)
    job_set = set(job_skills)
    
    # Calculate weighted similarity based on skill importance
    total_weight = 0.0
    matched_weight = 0.0
    
    for job_skill in job_set:
        weight = get_skill_weight(job_skill)
        total_weight += weight
        
        # Find best matching resume skill
        best_match_score = 0.0
        for resume_skill in resume_set:
            similarity = calculate_skill_similarity(job_skill, resume_skill)
            if similarity > best_match_score:
                best_match_score = similarity
        
        matched_weight += best_match_score * weight
    
    return matched_weight / total_weight if total_weight > 0 else 0.0

# NEW: Calculate skill similarity
def calculate_skill_similarity(skill1, skill2):
    s1 = skill1.lower()
    s2 = skill2.lower()
    
    # Exact match
    if s1 == s2:
        return 1.0
    
    # Contains match
    if s1 in s2 or s2 in s1:
        return 0.9
    
    # Common variations
    variations = {
        'js': 'javascript',
        'reactjs': 'react', 
        'angularjs': 'angular',
        'vuejs': 'vue',
        'nodejs': 'node.js',
        'springboot': 'spring boot'
    }
    
    for short, full in variations.items():
        if (s1 == short and s2 == full) or (s1 == full and s2 == short):
            return 0.95
    
    # Word-based similarity
    words1 = set(re.findall(r'\w+', s1))
    words2 = set(re.findall(r'\w+', s2))
    
    common_words = words1.intersection(words2)
    if common_words:
        return len(common_words) / max(len(words1), len(words2)) * 0.8
    
    return 0.0

# NEW: Get skill weight based on category
def get_skill_weight(skill):
    for category, skills in TECHNICAL_SKILLS.items():
        if skill in skills:
            return SKILL_IMPORTANCE_WEIGHTS.get(category, 1.0)
    return 1.0

# NEW: Categorize skills
def categorize_skills(skills):
    categories = {}
    for skill in skills:
        for category, skill_list in TECHNICAL_SKILLS.items():
            if skill in skill_list:
                if category not in categories:
                    categories[category] = []
                categories[category].append(skill)
    return categories

# NEW: Get primary domain from skill categories
def get_primary_domain(skill_categories):
    if not skill_categories:
        return "General"
    
    domain_scores = {}
    for category, skills in skill_categories.items():
        domain = get_domain_from_category(category)
        domain_scores[domain] = domain_scores.get(domain, 0) + len(skills)
    
    return max(domain_scores.items(), key=lambda x: x[1])[0] if domain_scores else "General"

# NEW: Map categories to domains
def get_domain_from_category(category):
    domain_mapping = {
        'programming_languages': 'IT & Software',
        'frontend_development': 'IT & Software',
        'backend_development': 'IT & Software', 
        'databases': 'IT & Software',
        'cloud_platforms': 'IT & Software',
        'ai_ml_data_science': 'IT & Software',
        'devops_tools': 'IT & Software',
        'mobile_development': 'IT & Software',
        'engineering': 'Engineering',
        'business_management': 'Business & Management',
        'sales_marketing': 'Business & Management',
        'healthcare_medical': 'Healthcare & Medical',
        'design_creative': 'Design & Creative'
    }
    return domain_mapping.get(category, 'Other')

# NEW: Get experience level
def get_experience_level(years):
    if years <= 2:
        return "Entry Level"
    elif years <= 5:
        return "Mid Level" 
    elif years <= 10:
        return "Senior Level"
    else:
        return "Executive Level"

# NEW: Get experience suitability
def get_experience_suitability(user_exp, min_exp, max_exp):
    if user_exp >= min_exp and user_exp <= max_exp:
        return "Perfect Match"
    elif user_exp < min_exp:
        return f"Needs {min_exp - user_exp} more years"
    else:
        return "Overqualified"

# NEW: Find matched skills between resume and job
def find_matched_skills(resume_skills, job_skills):
    matched = set()
    for job_skill in job_skills:
        for resume_skill in resume_skills:
            if calculate_skill_similarity(job_skill, resume_skill) > 0.7:
                matched.add(job_skill)
                break
    return list(matched)

# NEW: Find missing skills
def find_missing_skills(resume_skills, job_skills):
    matched = find_matched_skills(resume_skills, job_skills)
    return list(set(job_skills) - set(matched))

if __name__ == '__main__':
    print("Starting Enhanced AI Service on port 5000...")
    app.run(host='0.0.0.0', port=5000, debug=True)