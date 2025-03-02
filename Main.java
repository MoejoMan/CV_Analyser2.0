import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static String[] skills = {"Java", "Python", "C++", "C#", "SQL", "JavaScript", "PHP", "HTML", "Git", "AWS", "Docker", "Command Line", "Linux", "Shell", "CMD"};

    public static void main(String[] args) {
        DialogManager fileHandler = new DialogManager();
        List<String> resumeFiles = fileHandler.getResumes();

        // Get job descriptions first
        String[] jobDescriptionsArray = fileHandler.getJobDescriptions();
        List<String> jobDescriptions = (jobDescriptionsArray != null) ? Arrays.asList(jobDescriptionsArray) : List.of();

        // Create the database schema
        DatabaseManager.createSchema();

        // Load job descriptions into the database
        DatabaseManager.loadJobDescriptions(jobDescriptions);

        if (!fileHandler.start()) {
            System.out.println("Analysis cancelled.");
            return;
        }

        System.out.println("Job Descriptions Loaded:");
        for (String job : jobDescriptions) {
            System.out.println("- " + job);
        }

        // Process resumes
        for (String resumeFile : resumeFiles) {
            String resumeText = Fileutils.readFile(resumeFile);
            if (resumeText != null) {
                System.out.println("\nProcessing Resume: " + resumeFile);
                extractSkills(resumeText);
                extractEmail(resumeText);
                extractPhone(resumeText);
                extractName(resumeText);
                extractEducation(resumeText);
                extractWorkExperience(resumeText);

                for (String jobDescription : jobDescriptions) {
                    File jobFile = new File(jobDescription.trim());

                    if (jobFile.exists() && jobFile.isFile()) {
                        String jobDescriptionText = Fileutils.readFile(jobDescription.trim());
                        if (jobDescriptionText != null && !jobDescriptionText.trim().isEmpty()) {
                            insertResumeIntoDB(resumeText);
                            System.out.println("\n--- Processing Job Description from file ---");
                            extractSkills(jobDescriptionText);

                            double matchPercentage = RankingsManager.calculateSkillMatch(resumeText, jobDescriptionText, skills);
                            System.out.printf("Skill Match Percentage for Job Description file: %.2f%%\n", matchPercentage);
                        } else {
                            System.out.println("Failed to read or file is empty: " + jobDescription);
                        }
                    } else {
                        System.out.println("\n--- Processing Manual Job Description ---");
                        extractSkills(jobDescription);

                        double matchPercentage = RankingsManager.calculateSkillMatch(resumeText, jobDescription, skills);
                        System.out.printf("Skill Match Percentage for Manual Description: %.2f%%\n", matchPercentage);
                    }
                }
            } else {
                System.out.println("Failed to read the resume: " + resumeFile);
            }
        }
    }


public static String extractSkills(String resumeText) {
        StringBuilder skillsFound = new StringBuilder();
        for (String skill : skills) {
            if (resumeText.contains(skill)) {
                skillsFound.append(skill).append(", ");
                System.out.println(skill + " found in Resume");
            }
        }
        if (!skillsFound.isEmpty()) {
            String skillsString = skillsFound.toString();
            System.out.println("Skills found: " + skillsString);
            return skillsString;
        }
        System.out.println("No skills found.");
        return "No skills found.";
    }

    public static String extractEmail(String resumeText) {
        Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\\b");
        Matcher emailMatcher = emailPattern.matcher(resumeText);
        if (emailMatcher.find()) {
            String email = emailMatcher.group();
            System.out.println("Email found: " + email);
            return email;
        }
        return "No email found.";
    }

    public static String extractPhone(String resumeText) {
        Pattern phonePattern = Pattern.compile("\\+?\\d{1,4}?[-.\\s]?\\(?\\d{1,4}?\\)?[-.\\s]?\\d{3,4}[-.\\s]?\\d{4,6}");
        Matcher phoneMatcher = phonePattern.matcher(resumeText);
        if (phoneMatcher.find()) {
            String phone = phoneMatcher.group();
            System.out.println("Phone number found: " + phone);
            return phone;
        }
        System.out.println("No phone number found.");
        return "No phone number found.";
    }

    public static String extractName(String resumeText) {
        Pattern namePattern = Pattern.compile(
                "(?m)^(?:Name:)?\\s*([A-Z][a-z]+(?:\\s(?:de|la|van|von|[A-Z][a-z]+)){1,3})$"
        );
        Matcher nameMatcher = namePattern.matcher(resumeText);
        if (nameMatcher.find()) {
            String name = nameMatcher.group();
            System.out.println("Name found: " + name);
            return name;
        }
        System.out.println("No name found.");
        return "No name found.";
    }

    public static String extractEducation(String resumeText) {
        Pattern educationPattern = Pattern.compile("(?i)(education|academic background|qualifications|degree)(.*?)(?:experience|skills|work|certifications|$)", Pattern.DOTALL);
        Matcher educationMatcher = educationPattern.matcher(resumeText);

        if (educationMatcher.find()) {
            String educationSection = educationMatcher.group(2).trim();
            System.out.println("Education Section: " + educationSection);

            Pattern degreePattern = Pattern.compile(
                    "\\b(?:Bachelor|Master|PhD|Degree|BSc|MSc)?\\b.*?(?: in | of )?([A-Za-z .]+)?(?: at |, )?(\\bUniversity\\b|\\bCollege\\b|\\bInstitute\\b)?",
                    Pattern.CASE_INSENSITIVE
            );

            Matcher degreeMatcher = degreePattern.matcher(educationSection);

            StringBuilder foundDegrees = new StringBuilder();
            while (degreeMatcher.find()) {
                String degree = degreeMatcher.group().trim();
                foundDegrees.append(degree).append("; ");
                System.out.println("Degree: " + degree);
            }

            if (foundDegrees.length() > 0) {
                return foundDegrees.toString().trim();
            }
        }

        System.out.println("No Education found.");
        return "No Education found.";
    }


    public static void insertResumeIntoDB(String resumeText) {
        String insertResumeSQL = "INSERT INTO resumes (name, email, phone, skills, education, work_experience) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.connect(); PreparedStatement pstmt = conn.prepareStatement(insertResumeSQL)) {
            pstmt.setString(1, extractName(resumeText));  // Assume extract methods are available
            pstmt.setString(2, extractEmail(resumeText));
            pstmt.setString(3, extractPhone(resumeText));
            pstmt.setString(4, extractSkills(resumeText));
            pstmt.setString(5, extractEducation(resumeText));
            pstmt.setString(6, extractWorkExperience(resumeText));

            pstmt.executeUpdate();
            System.out.println("Resume inserted into database.");
        } catch (SQLException e) {
            System.out.println("Error inserting resume into database: " + e.getMessage());
        }
    }

    public static String extractWorkExperience(String resumeText) {
        Pattern workExperiencePattern = Pattern.compile("(?i)(experience|work history|professional experience)(.*?)(?:education|skills|certifications|$)", Pattern.DOTALL);
        Matcher workExperienceMatcher = workExperiencePattern.matcher(resumeText);
        if (workExperienceMatcher.find()) {
            String workExperienceSection = workExperienceMatcher.group(2);
            System.out.println("Work Experience Section: " + workExperienceSection);

            Pattern jobTitlePattern = Pattern.compile("(\\b(?:Software|Senior|Junior)?\\s?[A-Za-z]+(?:\\s[A-Za-z]+)?\\s?Developer|Engineer|Manager\\b)", Pattern.CASE_INSENSITIVE);
            Matcher jobTitleMatcher = jobTitlePattern.matcher(workExperienceSection);
            while (jobTitleMatcher.find()) {
                System.out.println("Job Title: " + jobTitleMatcher.group());
            }

            Pattern companyPattern = Pattern.compile(
                    "\\b(?:at|for|with|working for|employed by)\\s+([A-Z][a-zA-Z&.,-]+(?:\\s[A-Z][a-zA-Z&.,-]+)*)\\b",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher companyMatcher = companyPattern.matcher(workExperienceSection);
            while (companyMatcher.find()) {
                System.out.println("Company: " + companyMatcher.group(1));
            }
            return workExperienceSection;
        } else {
            System.out.println("No Work Experience found.");
        }
        return "No Work Experience found.";
    }
}
//test
//test3