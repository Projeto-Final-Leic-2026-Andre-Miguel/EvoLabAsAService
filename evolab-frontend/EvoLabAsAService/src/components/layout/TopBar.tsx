import { Link, Outlet, useNavigate } from "react-router-dom";
import styles from "./TopBar.module.css"; // Importação do teu CSS



export function TopBar() {

    const navigate = useNavigate();

    return (
        <nav className ={styles.topBarContainer}> 
            <div className ={styles.logo}>
               <Link to= "/" >EvoLab</Link> 
            </div>

            <div className= {styles.navLinks}>
                <Link to= "/">Home</Link>
                <Link to= "/projects">Projects</Link>
                <Link to= "/credentials">Credentials</Link>
                <Link to= "/config">Configuration files</Link>
                <Link to= "/contact">Contact</Link>


                <button className={styles.signInBtn} onClick={() => navigate('/register')}>
                      Sign In  
                </button>

            </div>
        </nav>

    )



}    