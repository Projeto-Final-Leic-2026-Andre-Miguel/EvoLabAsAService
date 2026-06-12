import { motion } from 'framer-motion';
import { useNavigate, useLocation } from 'react-router-dom';
import { useEffect, useState } from 'react';
import styles from './Home.module.css';
import { usePageTitle } from '../../hooks/usePageTitle';

export function Home() {
  usePageTitle('Home');
  const navigate = useNavigate();
  const location = useLocation();

  const [formState, setFormState] = useState({
    email: '',
    subject: '',
    description: ''
  });

  useEffect(() => {
    if (location.hash === '#contact') {
      const el = document.getElementById('contact');
      if (el) {
        el.scrollIntoView({ behavior: 'smooth' });
      }
    } else {
      window.scrollTo(0, 0);
    }
  }, [location]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formState.subject || !formState.description) return;
    
    // Simulate sending an email by opening the user's default email client
    const mailtoLink = `mailto:suporte@evolab.com?subject=${encodeURIComponent(formState.subject)}&body=${encodeURIComponent(
      `Subject: ${formState.subject}\n\nMessage or question:\n${formState.description}\n\nSent by: ${formState.email}`
    )}`;
    window.location.href = mailtoLink;
    
    setFormState({ email: '', subject: '', description: '' });
  };

  return (
    <div className={styles.homeContainer}>
      <motion.div 
        className={styles.heroSection}
        initial={{ opacity: 0, scale: 0.95 }} 
        animate={{ opacity: 1, scale: 1 }}   
        transition={{ duration: 0.6, ease: "easeOut" }}
      >
        <h1 className={styles.title}>Welcome to EvoLab</h1>
        <p className={styles.subtitle}>
          A web application where you can improve your algorithms through experimentation.
        </p>
        
        <button 
          className={styles.startBtn}
          onClick={() => navigate('/projects')}
        >
          Start Experimenting
        </button>
      </motion.div>

      {/* Contact & Creators Section */}
      <section id="contact" className={styles.contactSection}>
        <motion.div 
          className={styles.contactContent}
          initial={{ opacity: 0, y: 50 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
        >
          <h2 className={styles.sectionTitle}>Get in Touch</h2>
          <p className={styles.sectionSubtitle}>Meet the creators or send us your questions and feedback.</p>
          
          <div className={styles.contactGrid}>
            
            {/* Creators Sidebar */}
            <div className={styles.creatorsCard}>
              <h3>Meet the Creators</h3>
              
              <div className={styles.creatorProfile}>
                <div className={styles.creatorAvatar}>AV</div>
                <div className={styles.creatorInfo}>
                  <h4>André Vaz</h4>
                  <div className={styles.socialLinks}>
                    <a href="https://www.linkedin.com/in/andr%C3%A9-filipe-de-sousa-vaz-491632333/" target="_blank" rel="noreferrer" title="LinkedIn"> LinkedIn</a>
                    <a href="https://github.com/AndreVaz51585" target="_blank" rel="noreferrer" title="GitHub">GitHub</a>
                  </div>
                </div>
              </div>

              <div className={styles.creatorProfile}>
                <div className={styles.creatorAvatar}>MP</div>
                <div className={styles.creatorInfo}>
                  <h4>Miguel Pinto</h4>
                  <div className={styles.socialLinks}>
                    <a href="https://www.linkedin.com/in/miguel-morais-pinto/" target="_blank" rel="noreferrer" title="LinkedIn"> LinkedIn</a>
                    <a href="https://github.com/MiguelMPinto" target="_blank" rel="noreferrer" title="GitHub">GitHub</a>
                  </div>
                </div>
              </div>
            </div>

            {/* Email Support Form */}
            <form className={styles.contactForm} onSubmit={handleSubmit}>
              <h3>Send us a Message</h3>
              
              <div className={styles.formGroup}>
                <label>Your Email</label>
                <input 
                  type="email" 
                  placeholder="name@example.com" 
                  value={formState.email}
                  onChange={(e) => setFormState({...formState, email: e.target.value})}
                  required
                />
              </div>

              <div className={styles.formGroup}>
                <label>Subject</label>
                <input 
                  type="text" 
                  placeholder="Question or suggestion"
                  value={formState.subject}
                  onChange={(e) => setFormState({...formState, subject: e.target.value})}
                  required
                />
              </div>

              <div className={styles.formGroup}>
                <label>Description</label>
                <textarea 
                  placeholder="Describe your issue or suggestion..." 
                  value={formState.description}
                  onChange={(e) => setFormState({...formState, description: e.target.value})}
                  required
                  rows={4}
                />
              </div>

              <button type="submit" className={styles.submitBtn}>
                <span>✉️</span> Send Message
              </button>
            </form>

          </div>
        </motion.div>
      </section>
    </div>
  );
}
