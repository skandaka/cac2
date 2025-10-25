package com.example.cac3.data.database

import com.example.cac3.data.model.Opportunity
import com.example.cac3.data.model.OpportunityCategory
import java.util.Calendar

/**
 * Populates the database with all research data from the College Opportunity Hub research
 */
class DataPopulator(private val database: AppDatabase) {

    suspend fun populateAll() {
        val opportunities = mutableListOf<Opportunity>()

        // Add all opportunities from research
        opportunities.addAll(getCompetitions())
        opportunities.addAll(getEmploymentOpportunities())
        opportunities.addAll(getVolunteeringOpportunities())
        opportunities.addAll(getClubs())
        opportunities.addAll(getHonorSocieties())
        opportunities.addAll(getSummerPrograms())
        opportunities.addAll(getTestPrepResources())
        opportunities.addAll(getInternships())

        // Insert all at once
        database.opportunityDao().insertAll(opportunities)
    }

    // COMPETITIONS & PROGRAM DEADLINES
    private fun getCompetitions(): List<Opportunity> {
        return listOf(
            // Congressional App Challenge 2025
            Opportunity(
                title = "Congressional App Challenge 2025",
                description = "Create an app to be displayed in the U.S. Capitol. Teams up to 4 students compete to develop innovative mobile, web, or computer applications. Winning apps are showcased at the Capitol and highlighted on the House of Representatives website.",
                category = OpportunityCategory.COMPETITION,
                type = "Coding Competition",
                organizationName = "U.S. House of Representatives",
                website = "https://www.congressionalappchallenge.us",
                deadline = getTimestamp(2025, 10, 30, 12, 0), // October 30, 2025 at 12:00 PM EDT
                minGrade = 6,
                maxGrade = 12,
                cost = "FREE",
                costMin = 0.0,
                costMax = 0.0,
                scholarshipAvailable = false,
                requirements = "Grades 6-12, teams up to 4 students, must be in IL-08 Congressional District",
                applicationComponents = "Functional app submission, video demonstration, written description",
                benefits = "Winning app displayed in U.S. Capitol, national recognition, tech industry connections",
                tags = "coding,programming,technology,STEM,competition,app-development",
                isVirtual = true,
                transitAccessible = true,
                priority = 100,
            ),

            // Bank of America Student Leaders 2025
            Opportunity(
                title = "Bank of America Student Leaders 2025",
                description = "Paid 8-week nonprofit internship plus all-expenses-paid week in Washington D.C. Current juniors/seniors ages 16-18 with minimum 3.0 GPA and demonstrated community service work with local nonprofits and attend leadership summit in D.C. Nearly \$5,000 total compensation.",
                category = OpportunityCategory.SUMMER_PROGRAM,
                type = "Paid Internship + Leadership",
                organizationName = "Bank of America",
                website = "https://about.bankofamerica.com/student-leaders",
                deadline = getTimestamp(2025, 1, 15, 23, 59), // January 15, 2025 at 11:59 PM ET
                minGrade = 11,
                maxGrade = 12,
                minAge = 16,
                maxAge = 18,
                minGPA = 3.0,
                cost = "FREE",
                costMin = 0.0,
                costMax = 0.0,
                wage = "\$4,000-\$5,000 for 8 weeks",
                wageMin = 4000.0,
                wageMax = 5000.0,
                hoursPerWeek = "35 hours/week for 8 weeks",
                hoursPerWeekMin = 35,
                hoursPerWeekMax = 35,
                startDate = getTimestamp(2025, 6, 1),
                endDate = getTimestamp(2025, 7, 26),
                requirements = "Current junior/senior, ages 16-18, minimum 3.0 GPA, demonstrated community service",
                applicationComponents = "Online application, essay, recommendations, transcript",
                benefits = "Paid internship, all-expenses-paid D.C. trip July 21-26, leadership development, college prep, networking",
                tags = "leadership,internship,paid,service,nonprofit,Washington-DC",
                isVirtual = false,
                priority = 95
            ),

            // Intel ISEF 2025
            Opportunity(
                title = "Intel ISEF 2025 (International Science & Engineering Fair)",
                description = "The world's largest international pre-college science competition. Nearly \$9 million in prizes. Students must first compete in local/regional affiliated fairs. Top projects advance to international competition. Categories include engineering, computer science, biology, chemistry, physics, and more.",
                category = OpportunityCategory.COMPETITION,
                type = "Science Fair",
                organizationName = "Society for Science",
                website = "https://www.societyforscience.org/isef",
                deadline = getTimestamp(2025, 3, 31), // Local fairs December 2024 - March 2025
                startDate = getTimestamp(2025, 5, 10),
                endDate = getTimestamp(2025, 5, 16),
                minGrade = 9,
                maxGrade = 12,
                cost = "Varies by local fair",
                costMin = 0.0,
                costMax = 100.0,
                scholarshipAmount = "Up to \$9 million in total prizes",
                requirements = "Grades 9-12, must qualify through affiliated regional/local science fair",
                applicationComponents = "Research project, display board, research paper, presentation",
                benefits = "Up to \$9 million in prizes, college scholarships, internships, trips, international recognition",
                tags = "STEM,science,engineering,research,competition,scholarships",
                address = "Columbus, OH (2025 International Fair)",
                isVirtual = false,
                priority = 90,
            ),

            // MIT THINK Scholars
            Opportunity(
                title = "MIT THINK Scholars Program",
                description = "Massachusetts Institute of Technology funds innovative student research projects. Up to \$1,000 project funding plus MIT mentorship and all-expenses-paid February trip to MIT campus. Open to grades 9-12, U.S. residents. Present your research idea and receive support from MIT faculty.",
                category = OpportunityCategory.SUMMER_PROGRAM,
                type = "Research Funding",
                organizationName = "MIT",
                website = "https://think.mit.edu",
                deadline = getTimestamp(2026, 1, 1), // Opens November 2025
                minGrade = 9,
                maxGrade = 12,
                cost = "FREE",
                costMin = 0.0,
                costMax = 0.0,
                scholarshipAmount = "Up to \$1,000 project funding",
                requirements = "Grades 9-12, U.S. residents, innovative research proposal",
                applicationComponents = "Research proposal, budget, letters of recommendation",
                benefits = "\$1,000 funding, MIT mentorship, all-expenses-paid MIT visit in February, research experience",
                tags = "STEM,research,MIT,funding,innovation",
                address = "Cambridge, MA",
                isVirtual = false,
                priority = 92
            ),

            // Northwestern NHSI Cherubs Theater
            Opportunity(
                title = "Northwestern NHSI 'Cherubs' Theater Program",
                description = "Prestigious 5-week intensive theater program at Northwestern University. For rising seniors in top 30% of class only. Options for Theatre Arts and Film divisions. Highly selective program with renowned faculty. Early admission deadline in January for Theatre Arts/Film.",
                category = OpportunityCategory.SUMMER_PROGRAM,
                type = "Theater/Film Arts",
                organizationName = "Northwestern University",
                website = "https://nhsi.northwestern.edu/theatre-arts-division",
                deadline = getTimestamp(2025, 4, 25), // Regular deadline April 25, 2025
                startDate = getTimestamp(2025, 6, 29),
                endDate = getTimestamp(2025, 8, 2),
                minGrade = 11,
                maxGrade = 11, // Rising seniors only
                requirements = "Rising seniors, top 30% of class, passion for theater/film",
                cost = "\$60 application fee, financial aid available",
                costMin = 60.0,
                scholarshipAvailable = true,
                applicationComponents = "\$60 application fee, transcript, recommendations, essay, audition/portfolio",
                benefits = "Elite theater training, Northwestern faculty, college credit potential, networking",
                tags = "arts,theater,film,Northwestern,summer-program",
                address = "Evanston, IL",
                isVirtual = false,
                transitAccessible = true,
                paceRoutes = "208,236",
                priority = 85
            ),

            // University of Chicago Summer Programs
            Opportunity(
                title = "University of Chicago Summer Programs",
                description = "3-Week Immersion programs for ages 14+, grades 9-11. Two sessions available. Commuter option \$5,850. Most enrichment programs FREE. Explore college-level coursework at one of the nation's top universities. Priority deadline January 22, regular March 5, extended April 15.",
                category = OpportunityCategory.SUMMER_PROGRAM,
                type = "Academic Enrichment",
                organizationName = "University of Chicago",
                website = "https://summer.uchicago.edu/pre-college",
                deadline = getTimestamp(2025, 4, 15), // Extended deadline
                startDate = getTimestamp(2025, 6, 15),
                endDate = getTimestamp(2025, 8, 1),
                minGrade = 9,
                maxGrade = 11,
                minAge = 14,
                cost = "Most programs FREE, commuter option \$5,850",
                costMin = 0.0,
                costMax = 5850.0,
                scholarshipAvailable = true,
                requirements = "Ages 14+, grades 9-11, strong academic record",
                applicationComponents = "Online application, transcript, recommendations, essay",
                benefits = "College-level coursework, UChicago experience, academic exploration, many FREE options",
                tags = "academic,UChicago,summer-program,enrichment,FREE",
                address = "Chicago, IL",
                isVirtual = false,
                transitAccessible = true,
                paceRoutes = "208,600",
                priority = 88,
            ),

            // QuestBridge College Prep Scholars
            Opportunity(
                title = "QuestBridge College Prep Scholars",
                description = "For current juniors from low-income families (typically <\$65,000), top 5-10% of class. Over 3,600 Scholars receive full summer program scholarships to Yale, UChicago, Emory, Stanford plus \$1,000 Quest for Excellence Awards. Opens early February, deadline March 20.",
                category = OpportunityCategory.SUMMER_PROGRAM,
                type = "College Prep + Scholarships",
                organizationName = "QuestBridge",
                website = "https://www.questbridge.org",
                deadline = getTimestamp(2025, 3, 20, 23, 59), // March 20, 2025 at 11:59 PM PT
                minGrade = 11,
                maxGrade = 11,
                maxIncome = 65000,
                cost = "FREE",
                costMin = 0.0,
                costMax = 0.0,
                scholarshipAmount = "Full summer program scholarships + \$1,000 awards",
                requirements = "Current juniors, family income typically <\$65,000, top 5-10% of class",
                applicationComponents = "Extensive application, transcripts, test scores, essays, recommendations, financial info",
                benefits = "Full scholarships to elite summer programs (Yale, UChicago, Stanford, Emory), \$1,000 Quest for Excellence Awards, college admissions support",
                tags = "scholarships,low-income,college-prep,QuestBridge,summer-programs",
                isVirtual = false,
                priority = 98
            ),

            // HOBY Illinois
            Opportunity(
                title = "HOBY Illinois Leadership Seminar",
                description = "Hugh O'Brian Youth Leadership 4-day seminar for sophomores only. Two sessions: HOBY North (April-May) and HOBY Central South (June 19-22). School nominations typically February-March. \$400-\$500 cost, scholarships available through American Legion.",
                category = OpportunityCategory.SUMMER_PROGRAM,
                type = "Leadership Development",
                organizationName = "HOBY Illinois",
                website = "https://illinoisnorth.hoby.org",
                deadline = getTimestamp(2025, 3, 31), // School nominations typically Feb-March
                startDate = getTimestamp(2025, 6, 19),
                endDate = getTimestamp(2025, 6, 22),
                minGrade = 10,
                maxGrade = 10, // Sophomores only
                cost = "\$400-\$500",
                costMin = 400.0,
                costMax = 500.0,
                scholarshipAvailable = true,
                requirements = "Sophomores only, school nomination required",
                applicationComponents = "School nomination, application, recommendation",
                benefits = "Leadership development, networking, service projects, community building",
                tags = "leadership,sophomores,HOBY,service",
                isVirtual = false,
                priority = 75
            ),

            // Coca-Cola Scholars
            Opportunity(
                title = "Coca-Cola Scholars Program 2026",
                description = "150 Scholars receive \$20,000 each (\$3 million total). High school seniors, minimum 3.0 unweighted GPA. Opens August 1, deadline September 30, 2025 at 5:00 PM ET. One of the most prestigious scholarships in the nation.",
                category = OpportunityCategory.COMPETITION,
                type = "Scholarship",
                organizationName = "Coca-Cola Scholars Foundation",
                website = "https://www.coca-colascholarsfoundation.org",
                deadline = getTimestamp(2025, 9, 30, 17, 0), // September 30, 2025 at 5:00 PM ET
                minGrade = 12,
                maxGrade = 12,
                minGPA = 3.0,
                cost = "FREE",
                costMin = 0.0,
                costMax = 0.0,
                scholarshipAmount = "\$20,000 each (150 winners)",
                requirements = "High school seniors, minimum 3.0 unweighted GPA",
                applicationComponents = "Online application, transcript, activities list, essays",
                benefits = "\$20,000 scholarship, leadership development, national recognition, networking",
                tags = "scholarship,seniors,leadership,prestigious",
                isVirtual = true,
                transitAccessible = true,
                priority = 97,
            ),

            // QuestBridge National College Match
            Opportunity(
                title = "QuestBridge National College Match 2026",
                description = "Full four-year scholarships worth \$200,000+ to top colleges including Stanford, Yale, MIT, Northwestern, UChicago. For seniors from low-income families (typically <\$65,000). Opens late August, deadline September 30, 2025 at 11:59 PM PT.",
                category = OpportunityCategory.COMPETITION,
                type = "Full Scholarship",
                organizationName = "QuestBridge",
                website = "https://www.questbridge.org",
                deadline = getTimestamp(2025, 9, 30, 23, 59), // September 30, 2025 at 11:59 PM PT
                minGrade = 12,
                maxGrade = 12,
                maxIncome = 65000,
                cost = "FREE",
                costMin = 0.0,
                costMax = 0.0,
                scholarshipAmount = "Full four-year scholarships worth \$200,000+",
                requirements = "Seniors, family income typically <\$65,000, top academic achievement",
                applicationComponents = "Extensive application, transcripts, test scores (optional), essays, recommendations, financial info",
                benefits = "Full four-year scholarships to Stanford, Yale, MIT, Northwestern, UChicago, Princeton, others",
                tags = "scholarship,low-income,full-ride,QuestBridge,prestigious",
                isVirtual = true,
                transitAccessible = true,
                priority = 100,
            ),

            // Regeneron Science Talent Search
            Opportunity(
                title = "Regeneron Science Talent Search 2026",
                description = "America's oldest and most prestigious science competition for seniors. Top 300 receive \$2,000 each; Top 40 Finalists compete for \$1.8+ million, with \$250,000 first place. Deadline November 6, 2025 at 8:00 PM ET.",
                category = OpportunityCategory.COMPETITION,
                type = "Science Research",
                organizationName = "Society for Science",
                website = "https://www.societyforscience.org/regeneron-sts",
                deadline = getTimestamp(2025, 11, 6, 20, 0), // November 6, 2025 at 8:00 PM ET
                minGrade = 12,
                maxGrade = 12,
                cost = "FREE",
                costMin = 0.0,
                costMax = 0.0,
                scholarshipAmount = "Top 300: \$2,000 each; Top 40: compete for \$1.8M+ (\$250K first place)",
                requirements = "Seniors only, original scientific research project",
                applicationComponents = "Research paper, project description, recommendations, transcripts",
                benefits = "Up to \$250,000 in prizes, national recognition, college scholarships, research opportunities",
                tags = "STEM,research,science,seniors,prestigious,Regeneron",
                isVirtual = false,
                priority = 99,
            ),

            // Boys/Girls State Illinois
            Opportunity(
                title = "Boys State Illinois / Girls State Illinois 2026",
                description = "Week-long government and leadership program. American Legion nominations December 2025-February 2026 for current juniors. \$400-\$500 cost typically paid by Legion Post. Boys State offers over \$20,000 in scholarships. June 2026.",
                category = OpportunityCategory.SUMMER_PROGRAM,
                type = "Government/Leadership",
                organizationName = "American Legion Illinois",
                website = "https://www.illinoisboysstate.org",
                deadline = getTimestamp(2026, 2, 28), // Nominations typically Dec-Feb
                startDate = getTimestamp(2026, 6, 7),
                endDate = getTimestamp(2026, 6, 13),
                minGrade = 11,
                maxGrade = 11,
                cost = "\$400-\$500 (typically paid by American Legion Post)",
                costMin = 400.0,
                costMax = 500.0,
                scholarshipAmount = "Over \$20,000 in scholarships (Boys State)",
                requirements = "Current juniors, American Legion nomination",
                applicationComponents = "American Legion nomination, application",
                benefits = "Government experience, leadership development, college scholarships, networking",
                tags = "leadership,government,civics,American-Legion",
                isVirtual = false,
                priority = 80
            ),

            // UIUC Young Scholars
            Opportunity(
                title = "UIUC Young Scholars Program",
                description = "Completely FREE 6-week research program (June 20 – August 2, 2025) with fellowship payment. ~5% acceptance rate. For rising 10th-12th graders from IL/IN/KY/MI/MO/IA/WI. Applications February-March 2025. Work with UIUC faculty on cutting-edge research.",
                category = OpportunityCategory.SUMMER_PROGRAM,
                type = "Research Program",
                organizationName = "University of Illinois Urbana-Champaign",
                website = "https://admissions.illinois.edu/visit/summer-programs",
                deadline = getTimestamp(2025, 3, 15), // Applications February-March
                startDate = getTimestamp(2025, 6, 20),
                endDate = getTimestamp(2025, 8, 2),
                minGrade = 10,
                maxGrade = 12,
                cost = "FREE with fellowship payment",
                costMin = 0.0,
                costMax = 0.0,
                wage = "Fellowship payment provided",
                requirements = "Rising 10th-12th graders from IL/IN/KY/MI/MO/IA/WI, ~5% acceptance rate, strong academics",
                applicationComponents = "Application, transcripts, essays, recommendations",
                benefits = "FREE program, fellowship payment, UIUC research experience, faculty mentorship",
                tags = "STEM,research,UIUC,FREE,fellowship,competitive",
                address = "Urbana-Champaign, IL",
                isVirtual = false,
                priority = 93
            ),

            // Fermilab Saturday Morning Physics
            Opportunity(
                title = "Fermilab Saturday Morning Physics",
                description = "Completely FREE 11-week program for all high school students. Fall 2025: September 13 – November 22, Saturdays 9am-12pm. In-person (Batavia) or Zoom. Registration opens August. Learn physics from Fermilab scientists.",
                category = OpportunityCategory.SUMMER_PROGRAM,
                type = "Physics Enrichment",
                organizationName = "Fermilab",
                website = "https://education.fnal.gov/program/saturdaymorningphysics",
                deadline = getTimestamp(2025, 9, 1), // Registration opens August
                startDate = getTimestamp(2025, 9, 13),
                endDate = getTimestamp(2025, 11, 22),
                minGrade = 9,
                maxGrade = 12,
                cost = "FREE",
                costMin = 0.0,
                costMax = 0.0,
                hoursPerWeek = "3 hours, Saturdays only",
                hoursPerWeekMin = 3,
                hoursPerWeekMax = 3,
                requirements = "All high school students welcome",
                applicationComponents = "Registration form",
                benefits = "FREE physics education from Fermilab scientists, college-level content, in-person or virtual",
                tags = "STEM,physics,FREE,Fermilab,enrichment",
                address = "Batavia, IL",
                isVirtual = true,
                transitAccessible = true,
                priority = 85,
            )
        )
    }

    // Continue in next part...
    private fun getEmploymentOpportunities(): List<Opportunity> {
        return listOf(
            // Culver's
            Opportunity(
                title = "Culver's Crew Member",
                description = "Crew Member positions at Culver's. Some locations hire age 14-16 (most require 16). \$13-\$16/hour. Fast-paced restaurant environment with flexible hours for students. Known for excellent training and team culture.",
                category = OpportunityCategory.EMPLOYMENT,
                type = "Restaurant",
                organizationName = "Culver's",
                address = "4665 Hoffman Blvd, Hoffman Estates, IL",
                website = "https://www.culvers.com/careers",
                minAge = 14,
                wage = "\$13-\$16/hour",
                wageMin = 13.0,
                wageMax = 16.0,
                hoursPerWeek = "5-20 hours/week for students",
                hoursPerWeekMin = 5,
                hoursPerWeekMax = 20,
                workPermitRequired = true,
                requirements = "Age 14-16 (varies by location), work permit for ages 14-15",
                applicationComponents = "Online application at culvers.com/careers or in-person",
                benefits = "Flexible hours, meal discounts, team environment, training",
                tags = "restaurant,food-service,part-time,age-14,age-16",
                isVirtual = false,
                transitAccessible = true,
                paceRoutes = "208,554",
                priority = 70
            ),

            // Portillo's
            Opportunity(
                title = "Portillo's Crew Member",
                description = "Cashier, Order Taker, Crew Member positions. Age 16+. \$15+/hour. Chicago's iconic hot dog and Italian beef restaurant. Fast-paced, fun environment with great training. Located near Pace Bus Golf Road routes.",
                category = OpportunityCategory.EMPLOYMENT,
                type = "Restaurant",
                organizationName = "Portillo's",
                address = "611 E Golf Rd, Schaumburg, IL",
                website = "https://careers.portillos.com",
                contactPhone = "847-884-9020",
                minAge = 16,
                wage = "\$15+/hour",
                wageMin = 15.0,
                wageMax = 17.0,
                hoursPerWeek = "Part-time, flexible for students",
                hoursPerWeekMin = 10,
                hoursPerWeekMax = 25,
                requirements = "Age 16+",
                applicationComponents = "Online application at careers.portillos.com",
                benefits = "\$15+/hour, meal discounts, flexible hours, energetic team",
                tags = "restaurant,food-service,part-time,age-16,popular",
                isVirtual = false,
                transitAccessible = true,
                paceRoutes = "208",
                priority = 75
            ),

            // Chipotle
            Opportunity(
                title = "Chipotle Crew Member",
                description = "Four Schaumburg locations. Age 16+. \$12-\$13/hour, 5-20 hours/week for students, 50% meal discount. Flexible scheduling, advancement opportunities. Apply online at chipotle.com/careers.",
                category = OpportunityCategory.EMPLOYMENT,
                type = "Restaurant",
                organizationName = "Chipotle",
                address = "Multiple locations: 4600 Hoffman Blvd, 15 E Golf Rd, 2558 W Schaumburg Rd, Woodfield Mall",
                website = "https://www.chipotle.com/careers",
                minAge = 16,
                wage = "\$12-\$13/hour",
                wageMin = 12.0,
                wageMax = 13.0,
                hoursPerWeek = "5-20 hours/week for students",
                hoursPerWeekMin = 5,
                hoursPerWeekMax = 20,
                requirements = "Age 16+",
                applicationComponents = "Online application at chipotle.com/careers",
                benefits = "50% meal discount, flexible hours, advancement opportunities, tuition assistance",
                tags = "restaurant,food-service,part-time,age-16,multiple-locations",
                isVirtual = false,
                transitAccessible = true,
                paceRoutes = "208,554,604,905",
                priority = 73
            ),

            // Target
            Opportunity(
                title = "Target Team Member",
                description = "Age 16+. \$15.00/hour (company-wide minimum). Cashier, Sales Floor, Stock positions. Part-time 20-30 hours/week, 10% employee discount, 401(k). Excellent benefits for part-time workers. Apply at target.com/careers.",
                category = OpportunityCategory.EMPLOYMENT,
                type = "Retail",
                organizationName = "Target",
                address = "Multiple Schaumburg locations",
                website = "https://www.target.com/careers",
                minAge = 16,
                wage = "\$15.00/hour",
                wageMin = 15.0,
                wageMax = 15.0,
                hoursPerWeek = "20-30 hours/week part-time",
                hoursPerWeekMin = 20,
                hoursPerWeekMax = 30,
                requirements = "Age 16+",
                applicationComponents = "Online application at target.com/careers",
                benefits = "\$15/hour, 10% discount, 401(k), flexible hours, advancement",
                tags = "retail,part-time,age-16,benefits,well-paying",
                isVirtual = false,
                transitAccessible = true,
                paceRoutes = "208,554,905",
                priority = 80
            ),

            // AMC Theaters
            Opportunity(
                title = "AMC Theaters Team Member",
                description = "Ages 14-17 (varies by position). Minimum wage to \$10/hour. Usher, Cashier, Concessionist. FREE movies for employee + guest, food discounts. Evening/weekend shifts, 4-5 hours. Great first job opportunity!",
                category = OpportunityCategory.EMPLOYMENT,
                type = "Entertainment",
                organizationName = "AMC Theaters",
                address = "Woodfield Mall and other Schaumburg locations",
                website = "https://www.amctheatres.com/careers",
                minAge = 14,
                maxAge = 17,
                wage = "Minimum wage - \$10/hour",
                wageMin = 13.0,
                wageMax = 10.0,
                hoursPerWeek = "4-5 hour shifts, evenings/weekends",
                hoursPerWeekMin = 8,
                hoursPerWeekMax = 20,
                workPermitRequired = true,
                requirements = "Ages 14-17 depending on position, work permit for ages 14-15",
                applicationComponents = "Online application at amctheatres.com/careers",
                benefits = "FREE movies for employee + guest, food discounts, flexible hours",
                tags = "entertainment,movies,part-time,age-14,first-job",
                isVirtual = false,
                transitAccessible = true,
                paceRoutes = "208,554,905",
                priority = 78,
            ),

            // Woodfield Mall Retailers
            Opportunity(
                title = "Woodfield Mall Retail Positions",
                description = "Multiple retailers hiring age 16+: Hot Topic (\$15+/hr), Bath & Body Works (40% discount), Build-A-Bear, Aritzia (\$20-\$30/hr - highest), Hollister, American Eagle, Starbucks (\$15+/hr). Accessible via FREE Route 905 Trolley. Apply in person or online.",
                category = OpportunityCategory.EMPLOYMENT,
                type = "Retail",
                organizationName = "Woodfield Mall Retailers",
                address = "5 Woodfield Shopping Center, Schaumburg, IL",
                minAge = 16,
                wage = "\$15-\$30/hour depending on retailer",
                wageMin = 15.0,
                wageMax = 30.0,
                hoursPerWeek = "Part-time, flexible for students",
                hoursPerWeekMin = 10,
                hoursPerWeekMax = 25,
                requirements = "Age 16+, varies by retailer",
                applicationComponents = "Apply in person during non-peak hours (weekday mornings) or online through company websites",
                benefits = "Employee discounts (up to 40%), flexible hours, variety of positions",
                tags = "retail,mall,part-time,age-16,accessible,variety",
                isVirtual = false,
                transitAccessible = true,
                paceRoutes = "208,554,604,606,697,905",
                walkingDistance = "Direct access",
                priority = 85,
            ),

            // Schaumburg Park District
            Opportunity(
                title = "Schaumburg Park District Jobs",
                description = "Ages 14-16+ depending on position. Camp Counselor (16+), Lifeguard (15-16+, certification provided), Park Maintenance (16+, \$18/hr), Sports Officials. Peak hiring February-April for summer. \$15-\$18/hour.",
                category = OpportunityCategory.EMPLOYMENT,
                type = "Parks & Recreation",
                organizationName = "Schaumburg Park District",
                address = "505 N Springinsguth Rd, Schaumburg, IL",
                website = "https://www.schaumburgparkdistrict.com",
                contactPhone = "847-985-2115",
                minAge = 14,
                wage = "\$15-\$18/hour",
                wageMin = 15.0,
                wageMax = 18.0,
                hoursPerWeek = "Varies by position, summer seasonal",
                hoursPerWeekMin = 20,
                hoursPerWeekMax = 40,
                requirements = "Ages 14-16+ depending on position, certifications provided for lifeguards",
                applicationComponents = "Online application at schaumburgparkdistrict.com, peak hiring Feb-April",
                benefits = "Lifeguard certification provided, outdoor work, summer employment, good pay",
                tags = "parks,recreation,summer,camp,lifeguard,age-14,age-16",
                isVirtual = false,
                transitAccessible = true,
                paceRoutes = "208",
                priority = 82
            ),

            // Hoffman Estates Park District
            Opportunity(
                title = "Hoffman Estates Park District Jobs",
                description = "Ages 14-18+. Camp Counselor, Inclusion Support Staff, Lifeguard (certification provided), Sports Officials. \$15-\$18/hour. Peak hiring February-April for summer. Great for students interested in recreation, working with kids.",
                category = OpportunityCategory.EMPLOYMENT,
                type = "Parks & Recreation",
                organizationName = "Hoffman Estates Park District",
                address = "1685 W Higgins Rd, Hoffman Estates, IL",
                website = "https://www.heparks.org/jobs",
                contactPhone = "847-885-7500",
                minAge = 14,
                wage = "\$15-\$18/hour",
                wageMin = 15.0,
                wageMax = 18.0,
                hoursPerWeek = "Varies by position, summer seasonal",
                hoursPerWeekMin = 20,
                hoursPerWeekMax = 40,
                requirements = "Ages 14-18+ depending on position, certifications provided for lifeguards",
                applicationComponents = "Online application at heparks.org/jobs, peak hiring Feb-April",
                benefits = "Lifeguard certification provided, outdoor work, summer employment",
                tags = "parks,recreation,summer,camp,lifeguard,age-14",
                isVirtual = false,
                transitAccessible = true,
                priority = 80
            )
        )
    }

    // VOLUNTEERING OPPORTUNITIES
    private fun getVolunteeringOpportunities(): List<Opportunity> {
        return listOf(
            // Feed My Starving Children
            Opportunity(
                title = "Feed My Starving Children",
                description = "Pack meals for malnourished children worldwide. Age 5+ (ages 5-11 with parent, 12+ solo). 1.75-2 hour sessions, Monday-Saturday. FREE, donations encouraged. Most accessible opportunity via FREE Route 905 Trolley. Registration REQUIRED at fmsc.org.",
                category = OpportunityCategory.VOLUNTEERING,
                type = "Hunger Relief",
                organizationName = "Feed My Starving Children",
                address = "740 Wiley Farm Court, Schaumburg, IL (NEW 2025 location)",
                website = "https://www.fmsc.org/get-involved/volunteer",
                minAge = 5,
                cost = "FREE (donations encouraged)",
                costMin = 0.0,
                costMax = 0.0,
                hoursPerWeek = "1.75-2 hour sessions",
                hoursPerWeekMin = 2,
                hoursPerWeekMax = 2,
                requirements = "Ages 5+ (ages 5-11 must be with parent), registration REQUIRED online",
                applicationComponents = "Online registration at fmsc.org, no walk-ins",
                serviceHours = true,
                benefits = "Immediate service hour documentation, flexible scheduling, family-friendly, impactful work",
                tags = "volunteering,service-hours,flexible,FREE,accessible,family",
                isVirtual = false,
                transitAccessible = true,
                paceRoutes = "905",
                walkingDistance = "Short walk from Route 905 stop",
                priority = 95
            ),

            // Northwest Community Hospital
            Opportunity(
                title = "Northwest Community Hospital Volunteers",
                description = "Age 16+. Minimum 4 hours/week, 50 hours/semester (or 30 hours summer). Work: Information desks, physical therapy, nursing floors, patient transport. Health requirements: TB test, MMR, Varicella, flu vaccine, COVID-19. Timeline: 4-6 weeks from application to start.",
                category = OpportunityCategory.VOLUNTEERING,
                type = "Healthcare",
                organizationName = "Northwest Community Hospital",
                address = "800 W. Central Road, Arlington Heights, IL",
                website = "https://www.nch.org",
                contactEmail = "volunteer@nch.org",
                contactPhone = "847-618-4450",
                minAge = 16,
                cost = "FREE",
                costMin = 0.0,
                costMax = 0.0,
                hoursPerWeek = "Minimum 4 hours/week",
                hoursPerWeekMin = 4,
                totalHoursRequired = 50,
                requirements = "Age 16+, TB test, MMR, Varicella, flu vaccine, COVID-19 vaccine, 4-6 week application process",
                applicationComponents = "Email volunteer@nch.org, health screenings, orientation",
                serviceHours = true,
                benefits = "Healthcare experience, service hours after 50 hour minimum, patient interaction",
                tags = "volunteering,healthcare,hospital,service-hours,medical",
                isVirtual = false,
                transitAccessible = true,
                paceRoutes = "208",
                priority = 85
            ),

            // Schaumburg Township Library
            Opportunity(
                title = "Schaumburg Township Library Teen Volunteers",
                description = "Grades 9-12. Teen Volunteer Hours: 2-hour sessions monthly. Register online. Student Advisory Trustee (ages 15-18): Non-voting Library Board position, applications July 28-Aug 23, monthly meetings. Teen Advisory Group (TAG): ages 12-19, monthly meetings. Immediate service hour documentation.",
                category = OpportunityCategory.VOLUNTEERING,
                type = "Library/Education",
                organizationName = "Schaumburg Township District Library",
                address = "130 S. Roselle Rd, Schaumburg, IL",
                website = "https://schaumburg.libnet.info/events",
                contactPhone = "847-923-3439",
                minGrade = 9,
                maxGrade = 12,
                minAge = 12,
                maxAge = 19,
                cost = "FREE",
                costMin = 0.0,
                costMax = 0.0,
                hoursPerWeek = "2-hour sessions monthly",
                hoursPerWeekMin = 2,
                hoursPerWeekMax = 2,
                requirements = "Grades 9-12 for Teen Volunteer Hours, ages 12-19 for TAG, ages 15-18 for Student Trustee",
                applicationComponents = "Online registration at schaumburg.libnet.info/events",
                serviceHours = true,
                benefits = "Immediate service hour documentation, flexible, library experience, Student Trustee leadership role",
                tags = "volunteering,library,service-hours,flexible,leadership",
                isVirtual = false,
                transitAccessible = true,
                paceRoutes = "208",
                priority = 88,
            ),

            // WINGS Program
            Opportunity(
                title = "WINGS Program Volunteers",
                description = "Age 14+ (13 with parent at resale stores). Resale Store Volunteer (Schaumburg, Arlington Heights, Niles): Sort donations, organize clothing, cashier, pricing, displays. Flexible hours. Largest domestic violence housing agency in Illinois. Service hours provided.",
                category = OpportunityCategory.VOLUNTEERING,
                type = "Social Services",
                organizationName = "WINGS Program",
                address = "Resale stores: Schaumburg, Arlington Heights, Niles",
                website = "https://www.wingsprogram.com/volunteer",
                contactEmail = "bsrb@wingsprogram.com",
                contactPhone = "847-519-7820 ext. 216",
                minAge = 13,
                cost = "FREE",
                costMin = 0.0,
                costMax = 0.0,
                requirements = "Age 14+ (age 13 with parent at resale stores)",
                applicationComponents = "Contact Bruna Srb at bsrb@wingsprogram.com or 847-519-7820 ext. 216",
                serviceHours = true,
                benefits = "Flexible hours, meaningful cause (domestic violence support), retail skills",
                tags = "volunteering,social-services,flexible,service-hours,age-14",
                isVirtual = false,
                transitAccessible = true,
                priority = 75
            ),

            // Schaumburg Teen Center
            Opportunity(
                title = "Village of Schaumburg Teen Center",
                description = "Ages 12-19. Boys & Girls Club operated. Mon-Fri 2-7pm (school year), 1-6pm (summer). 231 S. Civic Drive. Accessible via FREE Route 905, stop at The Barn on Civic Drive. Activities, events, community service opportunities.",
                category = OpportunityCategory.VOLUNTEERING,
                type = "Community Center",
                organizationName = "Village of Schaumburg",
                address = "231 S. Civic Drive, Schaumburg, IL",
                contactPhone = "331-235-1353",
                minAge = 12,
                maxAge = 19,
                cost = "FREE",
                costMin = 0.0,
                costMax = 0.0,
                hoursPerWeek = "Mon-Fri 2-7pm (school year), 1-6pm (summer)",
                requirements = "Ages 12-19",
                serviceHours = true,
                benefits = "FREE activities, community service, social events, leadership opportunities",
                tags = "teen-center,community,FREE,accessible,social",
                isVirtual = false,
                transitAccessible = true,
                paceRoutes = "905",
                walkingDistance = "Direct stop",
                priority = 80
            ),

            // Schaumburg Park District - Spring Valley
            Opportunity(
                title = "Schaumburg Park District Spring Valley Volunteers",
                description = "Age 14+. Flexible projects: Garden maintenance (spring/fall), Special Events (GREATEST NEED, weekends year-round), Monarch butterfly education (May-September), Heritage Farm interpreter. Service hours provided.",
                category = OpportunityCategory.VOLUNTEERING,
                type = "Parks & Environment",
                organizationName = "Schaumburg Park District",
                address = "Spring Valley, Schaumburg, IL",
                website = "https://www.parkfun.com/spring-valley/volunteer",
                contactPhone = "847-985-2100",
                minAge = 14,
                cost = "FREE",
                costMin = 0.0,
                costMax = 0.0,
                requirements = "Age 14+, training provided for some roles",
                applicationComponents = "Apply at parkfun.com/spring-valley/volunteer",
                serviceHours = true,
                benefits = "Outdoor work, flexible projects, environmental education, service hours",
                tags = "volunteering,parks,environment,outdoors,service-hours,age-14",
                isVirtual = false,
                transitAccessible = true,
                priority = 70
            )
        )
    }

    // CLUBS (from Conant High School)
    private fun getClubs(): List<Opportunity> {
        return ConantClubsData.getAllClubs()
    }

    // HONOR SOCIETIES
    private fun getHonorSocieties(): List<Opportunity> {
        return listOf(
            // National Honor Society
            Opportunity(
                title = "National Honor Society (NHS)",
                description = "Membership based on outstanding scholarship, character, leadership, and service. Juniors and seniors with 3.0+ GPA eligible (chapters often require 3.5). NHS Scholarship deadline November 24, 2025 - 600 scholarships, \$3,200-\$25,000 for current NHS senior members.",
                category = OpportunityCategory.HONOR_SOCIETY,
                type = "Academic Honor Society",
                organizationName = "National Honor Society",
                website = "https://www.nationalhonorsociety.org",
                deadline = getTimestamp(2025, 11, 24, 17, 0),
                minGrade = 10,
                maxGrade = 12,
                minGPA = 3.0,
                cost = "\$20-\$50 per student (varies by chapter)",
                costMin = 20.0,
                costMax = 50.0,
                scholarshipAmount = "600 scholarships, \$3,200-\$25,000",
                requirements = "Minimum 3.0 GPA (often 3.5), grades 10-12, enrolled at school ≥1 semester, service hours",
                applicationComponents = "Faculty council 5-member majority vote on scholarship, service, leadership, character",
                graduationCord = true,
                benefits = "National recognition, NHS Scholarship eligibility, leadership development, service opportunities, graduation cords",
                tags = "honor-society,NHS,leadership,scholarship,service",
                isVirtual = false,
                priority = 85
            ),

            // Science National Honor Society
            Opportunity(
                title = "Science National Honor Society (SNHS)",
                description = "For students excelling in science. 3.0 overall GPA (unweighted), 3.5 science GPA (unweighted). Grades 10-12. Completed ≥1 honors/AP science, currently enrolled. 5-10 service hours/semester. Science competitions, STEM college recognition.",
                category = OpportunityCategory.HONOR_SOCIETY,
                type = "Science Honor Society",
                organizationName = "Science National Honor Society",
                website = "https://www.sciencenhs.org",
                minGrade = 10,
                maxGrade = 12,
                minGPA = 3.0,
                cost = "\$15-25/student",
                costMin = 15.0,
                costMax = 25.0,
                requirements = "3.0 overall GPA, 3.5 science GPA (unweighted), grades 10-12, completed ≥1 honors/AP science, 5-10 service hours/semester",
                serviceHours = true,
                graduationCord = true,
                benefits = "Science competitions, STEM college recognition, networking",
                tags = "honor-society,science,STEM,SNHS",
                isVirtual = false,
                priority = 75
            ),

            // Tri-M Music Honor Society
            Opportunity(
                title = "Tri-M Music Honor Society",
                description = "For music students. 3.0 GPA in music (A average some chapters), 3.0 overall. Grades typically 9-12. Must be enrolled in school music ensemble/class, 1-2 semesters participation. Over \$140,000 in scholarships since 1985. 84,000+ member network.",
                category = OpportunityCategory.HONOR_SOCIETY,
                type = "Music Honor Society",
                organizationName = "Tri-M Music Honor Society",
                website = "https://www.nafme.org",
                minGrade = 9,
                maxGrade = 12,
                minGPA = 3.0,
                cost = "Varies by chapter",
                requirements = "3.0 GPA in music, 3.0 overall, grades 9-12, enrolled in school music ensemble/class, 1-2 semesters participation",
                scholarshipAmount = "Over \$140,000 in scholarships",
                graduationCord = true,
                benefits = "Over \$140,000 scholarships (since 1985), yellow/light blue cord, 84,000+ member network",
                tags = "honor-society,music,Tri-M,arts",
                isVirtual = false,
                priority = 70
            ),

            // National Technical Honor Society
            Opportunity(
                title = "National Technical Honor Society (NTHS)",
                description = "For CTE students. 3.0 overall GPA (unweighted), A/B in CTE courses. Grades 11-12 (juniors/seniors in CTE program ≥2 semesters). Completed ≥2 CTE courses with A/B. Over \$300,000 annual scholarships. 1.2 million alumni.",
                category = OpportunityCategory.HONOR_SOCIETY,
                type = "CTE Honor Society",
                organizationName = "National Technical Honor Society",
                website = "https://www.nths.org",
                minGrade = 11,
                maxGrade = 12,
                minGPA = 3.0,
                cost = "\$30-40 one-time",
                costMin = 30.0,
                costMax = 40.0,
                scholarshipAmount = "Over \$300,000 annual scholarships",
                requirements = "3.0 overall GPA (unweighted), A/B in CTE courses, grades 11-12 (juniors/seniors in CTE program ≥2 semesters), completed ≥2 CTE courses with A/B",
                graduationCord = true,
                benefits = "Over \$300,000 annual scholarships, career development, industry connections, purple/white cord, 1.2 million alumni",
                tags = "honor-society,CTE,technical,career,NTHS",
                isVirtual = false,
                priority = 73
            ),

            // Mu Alpha Theta (Math)
            Opportunity(
                title = "Mu Alpha Theta (Math Honor Society)",
                description = "Mathematics honor society. 3.0 GPA in math required. Grades 9-12. Must have completed 2 years college prep math, enrolled in/completed 3rd year. 10 tutoring hours annually typical. National math competitions (6 levels), scholarships (Kalin, Andree Awards). Yellow/light blue cord.",
                category = OpportunityCategory.HONOR_SOCIETY,
                type = "Math Honor Society",
                organizationName = "Mu Alpha Theta",
                website = "https://www.mualphatheta.org",
                minGrade = 9,
                maxGrade = 12,
                minGPA = 3.0,
                cost = "\$25-35 one-time",
                costMin = 25.0,
                costMax = 35.0,
                requirements = "3.0 GPA in math, grades 9-12, completed 2 years college prep math, enrolled in/completed 3rd year, 10 tutoring hours annually",
                serviceHours = true,
                scholarshipAvailable = true,
                graduationCord = true,
                benefits = "National math competitions (6 levels), scholarships (Kalin, Andree Awards), yellow/light blue cord, tutoring experience",
                tags = "honor-society,math,Mu-Alpha-Theta,STEM,tutoring",
                isVirtual = false,
                priority = 72
            ),

            // National English Honor Society
            Opportunity(
                title = "National English Honor Society (NEHS)",
                description = "English honor society. 3.0 overall GPA, 3.0 English GPA. Grades 10-12 (no freshmen). Completed ≥2 semesters English. Literacy-focused service. Scholarships, publication opportunities, royal blue/gold cord, 2,900+ chapter network.",
                category = OpportunityCategory.HONOR_SOCIETY,
                type = "English Honor Society",
                organizationName = "National English Honor Society",
                website = "https://www.nehs.us",
                minGrade = 10,
                maxGrade = 12,
                minGPA = 3.0,
                cost = "\$25-40 one-time",
                costMin = 25.0,
                costMax = 40.0,
                requirements = "3.0 overall GPA, 3.0 English GPA, grades 10-12 (no freshmen), completed ≥2 semesters English, literacy service",
                serviceHours = true,
                scholarshipAvailable = true,
                graduationCord = true,
                benefits = "Scholarships, publication opportunities, royal blue/gold cord, 2,900+ chapter network, literacy service",
                tags = "honor-society,English,literature,writing,NEHS",
                isVirtual = false,
                priority = 70
            ),

            // Rho Kappa (Social Studies)
            Opportunity(
                title = "Rho Kappa (Social Studies Honor Society)",
                description = "Social studies honor society. 3.0 overall GPA (unweighted), 3.5 social studies GPA (unweighted). Grades 11-12 only (juniors/seniors). Completed ≥2 core social studies courses (4 semesters), prepared to complete ≥3 total (6 semesters). Social studies recognition, civic engagement.",
                category = OpportunityCategory.HONOR_SOCIETY,
                type = "Social Studies Honor Society",
                organizationName = "Rho Kappa",
                website = "https://www.socialstudies.org/rhokappa",
                minGrade = 11,
                maxGrade = 12,
                minGPA = 3.0,
                cost = "\$15-25",
                costMin = 15.0,
                costMax = 25.0,
                requirements = "3.0 overall GPA (unweighted), 3.5 social studies GPA (unweighted), grades 11-12 only, completed ≥2 core social studies courses (4 semesters)",
                graduationCord = true,
                benefits = "Social studies recognition, civic engagement opportunities, graduation cord",
                tags = "honor-society,social-studies,history,civics,Rho-Kappa",
                isVirtual = false,
                priority = 68
            ),

            // National Art Honor Society
            Opportunity(
                title = "National Art Honor Society (NAHS)",
                description = "Art honor society. 3.0 GPA in art (often 3.0 overall too). Grades 9-12. Completed ≥1 semester art, currently enrolled in art. 5-10 service hours annually. Up to \$8,000 art school scholarships, national exhibitions, rainbow graduation cord (after 2 years), 54,000+ member network.",
                category = OpportunityCategory.HONOR_SOCIETY,
                type = "Art Honor Society",
                organizationName = "National Art Honor Society",
                website = "https://www.arteducators.org/nahs",
                minGrade = 9,
                maxGrade = 12,
                minGPA = 3.0,
                cost = "\$5/student annually",
                costMin = 5.0,
                costMax = 5.0,
                requirements = "3.0 GPA in art (often 3.0 overall), grades 9-12, completed ≥1 semester art, currently enrolled in art, 5-10 service hours annually",
                serviceHours = true,
                scholarshipAmount = "Up to \$8,000 art school scholarships",
                graduationCord = true,
                benefits = "Up to \$8,000 art school scholarships, national exhibitions, rainbow graduation cord (after 2 years), 54,000+ member network",
                tags = "honor-society,art,visual-arts,NAHS,creativity",
                isVirtual = false,
                priority = 69
            ),

            // Sociedad Honoraria Hispánica (Spanish)
            Opportunity(
                title = "Sociedad Honoraria Hispánica (Spanish Honor Society)",
                description = "Spanish honor society. 3.0 Spanish GPA (3.5 recommended), 3.0 overall. Grade 10+. Completed ≥3 semesters Spanish/Portuguese (typically Spanish 3+), currently enrolled. 10 hours community service annually. Over \$160,000 scholarships/awards (60 Joseph Adams Senior Scholarships, 24 Bertie Green Travel Awards for juniors). Red/gold cord, 2,900+ chapters.",
                category = OpportunityCategory.HONOR_SOCIETY,
                type = "World Language Honor Society",
                organizationName = "Sociedad Honoraria Hispánica",
                website = "https://www.aatspshh.org",
                minGrade = 10,
                maxGrade = 12,
                minGPA = 3.0,
                cost = "Varies by chapter",
                requirements = "3.0 Spanish GPA (3.5 recommended), 3.0 overall, grade 10+, completed ≥3 semesters Spanish (Spanish 3+), currently enrolled, 10 hours service annually",
                serviceHours = true,
                scholarshipAmount = "Over \$160,000 scholarships/awards (60 Joseph Adams Senior Scholarships, 24 Bertie Green Travel Awards)",
                graduationCord = true,
                benefits = "Over \$160,000 scholarships/awards, red/gold cord, 2,900+ chapters, travel awards for juniors",
                tags = "honor-society,Spanish,world-language,Hispanic,SHH",
                isVirtual = false,
                priority = 71
            ),

            // Société Honoraire de Français (French)
            Opportunity(
                title = "Société Honoraire de Français (French Honor Society)",
                description = "French honor society. 3.3-3.6 French GPA (A- or 90%), 3.0 overall. Grades 10-12 (exception for 9th who met requirements in middle school). Enrolled in ≥4th semester French (French II second semester+), remain enrolled. National writing contest, travel grants, scholarships, tri-colored (blue/white/red) cord.",
                category = OpportunityCategory.HONOR_SOCIETY,
                type = "World Language Honor Society",
                organizationName = "Société Honoraire de Français",
                website = "https://www.frenchteachers.org/shf",
                minGrade = 10,
                maxGrade = 12,
                minGPA = 3.0,
                cost = "Varies by chapter",
                requirements = "3.3-3.6 French GPA (A- or 90%), 3.0 overall, grades 10-12, enrolled in ≥4th semester French (French II second semester+), remain enrolled",
                scholarshipAvailable = true,
                graduationCord = true,
                benefits = "National writing contest, travel grants, scholarships, tri-colored (blue/white/red) cord",
                tags = "honor-society,French,world-language,SHF",
                isVirtual = false,
                priority = 69
            ),

            // Delta Epsilon Phi (German)
            Opportunity(
                title = "Delta Epsilon Phi (German Honor Society)",
                description = "German honor society. 3.3 (87%) or 3.6 German GPA, 3.0 overall. Grades 9-12. Completed 3 semesters German. \$25 chapter activation (one-time). Scholarships, black/red/gold cord.",
                category = OpportunityCategory.HONOR_SOCIETY,
                type = "World Language Honor Society",
                organizationName = "Delta Epsilon Phi",
                website = "https://www.aatg.org/dep",
                minGrade = 9,
                maxGrade = 12,
                minGPA = 3.0,
                cost = "\$25 chapter activation (one-time)",
                costMin = 25.0,
                costMax = 25.0,
                requirements = "3.3 (87%) or 3.6 German GPA, 3.0 overall, grades 9-12, completed 3 semesters German",
                scholarshipAvailable = true,
                graduationCord = true,
                benefits = "Scholarships, black/red/gold cord",
                tags = "honor-society,German,world-language,Delta-Epsilon-Phi",
                isVirtual = false,
                priority = 67
            )
        )
    }

    // SUMMER PROGRAMS
    private fun getSummerPrograms(): List<Opportunity> {
        return listOf(
            // IMSA Summer Programs
            Opportunity(
                title = "IMSA Summer Programs",
                description = "Computer Science+X Camp June 22-27, 2025 (\$1,280, scholarships available). RISE Program year-long online research (\$1,280). Illinois Math and Science Academy programs for enrichment and research.",
                category = OpportunityCategory.SUMMER_PROGRAM,
                type = "STEM Enrichment",
                organizationName = "Illinois Math and Science Academy",
                website = "https://www.imsa.edu/youth-outreach/summerimsa",
                startDate = getTimestamp(2025, 6, 22),
                endDate = getTimestamp(2025, 6, 27),
                cost = "\$1,280 (scholarships available)",
                costMin = 1280.0,
                costMax = 1280.0,
                scholarshipAvailable = true,
                requirements = "Varies by program",
                benefits = "STEM enrichment, IMSA experience, research opportunities",
                tags = "STEM,IMSA,summer-program,computer-science,research",
                address = "Aurora, IL",
                isVirtual = false,
                priority = 72
            ),

            // Young Eagles Day
            Opportunity(
                title = "Young Eagles Day (FREE Flights)",
                description = "FREE airplane flights for ages 8-17! First Saturday monthly, May-October, 8:30am-12pm at Schaumburg Regional Airport. MUST register in advance at youngeaglesday.org. First come, first served. Operated by EAA Chapter 790. Incredible aviation experience!",
                category = OpportunityCategory.SUMMER_PROGRAM,
                type = "Aviation Experience",
                organizationName = "EAA Chapter 790 / Schaumburg Regional Airport",
                website = "https://youngeaglesday.org",
                address = "905 W. Irving Park Road, Schaumburg, IL",
                contactPhone = "847-923-3778",
                minAge = 8,
                maxAge = 17,
                cost = "FREE",
                costMin = 0.0,
                costMax = 0.0,
                startDate = getTimestamp(2025, 5, 3),
                endDate = getTimestamp(2025, 10, 4),
                requirements = "Ages 8-17, MUST register in advance at youngeaglesday.org, first come first served",
                applicationComponents = "Online registration at youngeaglesday.org",
                benefits = "FREE airplane flight, aviation experience, inspiring opportunity",
                tags = "aviation,FREE,flights,Young-Eagles,summer,hidden-gem",
                isVirtual = false,
                requiresCar = true,
                priority = 88
            )
        )
    }

    // TEST PREP RESOURCES
    private fun getTestPrepResources(): List<Opportunity> {
        return listOf(
            // Whatever It Takes (UChicago)
            Opportunity(
                title = "Whatever It Takes - FREE SAT/ACT Prep (UChicago)",
                description = "Completely FREE 10-week SAT/ACT prep program. Sundays 10am-1:30pm at UChicago Hyde Park. One Kaplan textbook required (purchase). Winter session starts January. Excellent free test prep resource!",
                category = OpportunityCategory.TEST_PREP,
                type = "Test Preparation",
                organizationName = "University of Chicago - Whatever It Takes",
                website = "https://whateverittakes.uchicago.edu",
                address = "Hyde Park, Chicago, IL",
                cost = "FREE (Kaplan textbook required)",
                costMin = 0.0,
                costMax = 40.0,
                hoursPerWeek = "3.5 hours/week, Sundays only",
                hoursPerWeekMin = 3,
                hoursPerWeekMax = 4,
                requirements = "High school students",
                benefits = "Completely FREE SAT/ACT prep, Kaplan curriculum, UChicago location",
                tags = "test-prep,SAT,ACT,FREE,UChicago",
                isVirtual = false,
                transitAccessible = true,
                paceRoutes = "600",
                priority = 90
            ),

            // Schoolhouse.world SAT Bootcamp
            Opportunity(
                title = "Schoolhouse.world FREE SAT Bootcamp",
                description = "FREE 4-week virtual small-group SAT tutoring with 95th percentile+ peer tutors. Completely online, accessible from anywhere. Excellent free resource for SAT preparation.",
                category = OpportunityCategory.TEST_PREP,
                type = "Test Preparation",
                organizationName = "Schoolhouse.world",
                website = "https://schoolhouse.world/sat-bootcamp",
                cost = "FREE",
                costMin = 0.0,
                costMax = 0.0,
                requirements = "High school students",
                benefits = "FREE SAT prep, peer tutoring, virtual/flexible",
                tags = "test-prep,SAT,FREE,virtual,tutoring",
                isVirtual = true,
                transitAccessible = true,
                priority = 85,
            )
        )
    }

    // INTERNSHIPS
    private fun getInternships(): List<Opportunity> {
        return listOf(
            // Abbott High School STEM Internship
            Opportunity(
                title = "Abbott High School STEM Internship",
                description = "First-of-its-kind with ACE-recommended college credit (up to 2 hours). Students from partner high schools only near Abbott facilities. Must be from partnering school district (60%+ diverse backgrounds, 50%+ young women). Hands-on STEM work with Abbott engineers/scientists, paid internship, Yellow Belt certification. Students cannot apply directly - contact school counselor.",
                category = OpportunityCategory.INTERNSHIP,
                type = "STEM Internship",
                organizationName = "Abbott",
                website = "https://www.abbott.com/careers/students.html",
                address = "Abbott Park, IL (northwest Chicago suburbs)",
                wage = "Paid internship",
                collegeCredit = true,
                requirements = "Must be from partnering school district near Abbott facilities (60%+ diverse backgrounds, 50%+ young women). Cannot apply directly - contact school counselor",
                applicationComponents = "School counselor nomination/application",
                benefits = "Paid internship, up to 2 college credit hours, Yellow Belt certification, hands-on STEM work with Abbott professionals, resume/interview skills",
                tags = "internship,STEM,paid,Abbott,college-credit,diversity",
                isVirtual = false,
                priority = 92,
            ),

            // Art Institute of Chicago Teen Programs
            Opportunity(
                title = "Art Institute of Chicago Teen Programs",
                description = "Teen Council: Paid, 15 youth artists/leaders, Saturdays 3-4 hours/week. Summer Internships: Paid, age 16+, Chicago teens only (apply through After School Matters in spring). FREE admission for Chicago teens under 18. Exuberus Teen Night: FREE annual 4-hour party event.",
                category = OpportunityCategory.INTERNSHIP,
                type = "Arts Internship",
                organizationName = "Art Institute of Chicago",
                website = "https://www.artic.edu/learn-with-us/teens/opportunities-for-teens",
                contactEmail = "teens@artic.edu",
                contactPhone = "312-857-7161",
                address = "Chicago, IL",
                minAge = 16,
                wage = "Paid positions",
                cost = "FREE admission for Chicago teens under 18",
                costMin = 0.0,
                costMax = 0.0,
                requirements = "Age 16+ for summer internships, Chicago teens only. Teen Council application through website. Summer internships through After School Matters (spring).",
                applicationComponents = "Teen Council: Apply through website. Summer Internships: After School Matters application in spring",
                benefits = "Paid positions, arts experience, museum access, leadership development",
                tags = "internship,arts,paid,museum,Chicago",
                isVirtual = false,
                transitAccessible = true,
                paceRoutes = "600",
                priority = 78
            )
        )
    }

    // Helper function to create timestamps
    private fun getTimestamp(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day, hour, minute, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
