import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import styles from './Home.module.css';

export function Home() {
  const navigate = useNavigate();

  return (
    <div className={styles.homeContainer}>
      <motion.div 
        className={styles.heroSection}
        initial={{ opacity: 0, x: -50 }} 
        animate={{ opacity: 1, x: 0 }}   
        transition={{ duration: 1.5, ease: "easeOut" }}
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
    </div>
  );
}
